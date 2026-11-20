package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.MaterialConsumptionDto;
import com.nittrichy.ordertoaudit.dto.ProductionRunRequestDto;
import com.nittrichy.ordertoaudit.dto.ProductionRunResponseDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.ProductionStatus;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionService {

    private final ProductionRunRepository productionRunRepository;
    private final ProductRepository productRepository;
    private final BOMRepository bomRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;
    private final InventoryService inventoryService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<ProductionRunResponseDto> getAllProductionRuns() {
        return productionRunRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductionRunResponseDto getProductionRunById(Long id) {
        ProductionRun run = productionRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionRun", "id", id));
        return mapToDto(run);
    }

    @Transactional
    public ProductionRunResponseDto recordProductionRun(ProductionRunRequestDto request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        BOM bom;
        if (request.getBomId() != null) {
            bom = bomRepository.findById(request.getBomId())
                    .orElseThrow(() -> new ResourceNotFoundException("BOM", "id", request.getBomId()));
        } else {
            List<BOM> activeBoms = bomRepository.findAllByProductIdAndActiveTrue(product.getId());
            if (activeBoms.isEmpty()) {
                throw new BusinessRuleException("No active BOM found for product: " + product.getName());
            }
            if (activeBoms.size() > 1) {
                throw new BusinessRuleException("Multiple active BOMs found for product: " + product.getName());
            }
            bom = activeBoms.get(0);
        }

        if (!bom.getProduct().getId().equals(product.getId())) {
            throw new BusinessRuleException("BOM " + bom.getBomCode() + " does not belong to product " + product.getProductCode());
        }

        BigDecimal multiplier = request.getBatchMultiplier() != null ? request.getBatchMultiplier() : BigDecimal.ONE;
        BigDecimal expectedOutput = bom.getBatchSize().multiply(multiplier);
        BigDecimal actualGoodOutput = request.getActualGoodOutput();

        if (multiplier.compareTo(BigDecimal.ZERO) <= 0 || expectedOutput.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Batch multiplier and expected output must be greater than zero");
        }

        if (actualGoodOutput.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Actual good output cannot be negative");
        }

        // Calculate yield variance: ((actual - expected) / expected) * 100
        BigDecimal yieldDiff = actualGoodOutput.subtract(expectedOutput);
        BigDecimal yieldVariancePct = yieldDiff.divide(expectedOutput, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        boolean yieldFlag = yieldVariancePct.abs().compareTo(new BigDecimal("3.00")) > 0;

        String runNumber = request.getRunNumber();
        if (runNumber == null || runNumber.isBlank()) {
            runNumber = "PR-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        User currentUser = authService.getCurrentAuthenticatedUser();

        BigDecimal labourCost = request.getLabourCost() != null ? request.getLabourCost() : BigDecimal.ZERO;
        BigDecimal overheadCost = request.getOverheadCost() != null ? request.getOverheadCost() : BigDecimal.ZERO;

        ProductionRun run = ProductionRun.builder()
                .runNumber(runNumber)
                .product(product)
                .bom(bom)
                .runDate(request.getRunDate() != null ? request.getRunDate() : LocalDate.now())
                .batchMultiplier(multiplier)
                .expectedOutput(expectedOutput)
                .actualGoodOutput(actualGoodOutput)
                .yieldVariancePercentage(yieldVariancePct)
                .varianceFlag(yieldFlag)
                .labourCost(labourCost)
                .overheadCost(overheadCost)
                .status(ProductionStatus.COMPLETED)
                .supervisor(currentUser)
                .notes(request.getNotes())
                .build();

        // Map request consumptions by item id for quick lookup
        Map<Long, MaterialConsumptionDto> requestedConsumptionsByRmId = new HashMap<>();
        Map<Long, MaterialConsumptionDto> requestedConsumptionsByPmId = new HashMap<>();

        if (request.getConsumptions() != null) {
            for (MaterialConsumptionDto c : request.getConsumptions()) {
                if (c.getItemType() == ItemType.RAW_MATERIAL && c.getRawMaterialId() != null) {
                    requestedConsumptionsByRmId.put(c.getRawMaterialId(), c);
                } else if (c.getItemType() == ItemType.PACKAGING_MATERIAL && c.getPackagingMaterialId() != null) {
                    requestedConsumptionsByPmId.put(c.getPackagingMaterialId(), c);
                }
            }
        }

        BigDecimal totalRmCost = BigDecimal.ZERO;
        BigDecimal totalPmCost = BigDecimal.ZERO;

        // Process BOM items
        for (BOMItem item : bom.getItems()) {
            BigDecimal plannedQty = item.getStandardQuantity().multiply(multiplier);

            if (item.getItemType() == ItemType.RAW_MATERIAL) {
                RawMaterial rm = item.getRawMaterial();
                MaterialConsumptionDto customConsumption = requestedConsumptionsByRmId.get(rm.getId());

                BigDecimal actualQty = customConsumption != null && customConsumption.getActualQuantity() != null
                        ? customConsumption.getActualQuantity() : plannedQty;

                BigDecimal damagedQty = customConsumption != null && customConsumption.getDamagedQuantity() != null
                        ? customConsumption.getDamagedQuantity() : BigDecimal.ZERO;

                validateConsumption(actualQty, damagedQty, rm.getName());

                BigDecimal totalDeduction = actualQty.add(damagedQty);

                // Consumption variance: ((actual - planned) / planned) * 100
                BigDecimal consDiff = actualQty.subtract(plannedQty);
                BigDecimal consVariancePct = BigDecimal.ZERO;
                if (plannedQty.compareTo(BigDecimal.ZERO) > 0) {
                    consVariancePct = consDiff.divide(plannedQty, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                boolean consFlag = consVariancePct.abs().compareTo(new BigDecimal("3.00")) > 0;
                BigDecimal unitCost = rm.getWeightedAvgCost() != null ? rm.getWeightedAvgCost() : BigDecimal.ZERO;
                BigDecimal totalItemCost = totalDeduction.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
                totalRmCost = totalRmCost.add(totalItemCost);

                // Deduct from stock
                inventoryService.recordRawMaterialMovement(
                        rm,
                        totalDeduction.negate(),
                        MovementType.PRODUCTION_ISSUE,
                        unitCost,
                        ReferenceType.PRODUCTION_RUN,
                        null,
                        runNumber,
                        "Production issue for run " + runNumber,
                        currentUser
                );

                MaterialConsumption consumption = MaterialConsumption.builder()
                        .itemType(ItemType.RAW_MATERIAL)
                        .rawMaterial(rm)
                        .plannedQuantity(plannedQty)
                        .actualQuantity(actualQty)
                        .damagedQuantity(damagedQty)
                        .unitCost(unitCost)
                        .totalCost(totalItemCost)
                        .variancePercentage(consVariancePct)
                        .varianceFlag(consFlag)
                        .build();

                run.addConsumption(consumption);

            } else if (item.getItemType() == ItemType.PACKAGING_MATERIAL) {
                PackagingMaterial pm = item.getPackagingMaterial();
                MaterialConsumptionDto customConsumption = requestedConsumptionsByPmId.get(pm.getId());

                BigDecimal actualQty = customConsumption != null && customConsumption.getActualQuantity() != null
                        ? customConsumption.getActualQuantity() : plannedQty;

                BigDecimal damagedQty = customConsumption != null && customConsumption.getDamagedQuantity() != null
                        ? customConsumption.getDamagedQuantity() : BigDecimal.ZERO;

                validateConsumption(actualQty, damagedQty, pm.getName());

                BigDecimal totalDeduction = actualQty.add(damagedQty);

                BigDecimal consDiff = totalDeduction.subtract(plannedQty);
                BigDecimal consVariancePct = BigDecimal.ZERO;
                if (plannedQty.compareTo(BigDecimal.ZERO) > 0) {
                    consVariancePct = consDiff.divide(plannedQty, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                boolean consFlag = consVariancePct.abs().compareTo(new BigDecimal("3.00")) > 0;
                BigDecimal unitCost = pm.getUnitCost() != null ? pm.getUnitCost() : BigDecimal.ZERO;
                BigDecimal totalItemCost = totalDeduction.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
                totalPmCost = totalPmCost.add(totalItemCost);

                // Deduct from stock
                inventoryService.recordPackagingMaterialMovement(
                        pm,
                        totalDeduction.negate(),
                        damagedQty.compareTo(BigDecimal.ZERO) > 0 ? MovementType.PRODUCTION_DAMAGE : MovementType.PRODUCTION_ISSUE,
                        unitCost,
                        ReferenceType.PRODUCTION_RUN,
                        null,
                        runNumber,
                        "Packaging consumption for run " + runNumber + (damagedQty.compareTo(BigDecimal.ZERO) > 0 ? " (includes " + damagedQty + " damaged)" : ""),
                        currentUser
                );

                MaterialConsumption consumption = MaterialConsumption.builder()
                        .itemType(ItemType.PACKAGING_MATERIAL)
                        .packagingMaterial(pm)
                        .plannedQuantity(plannedQty)
                        .actualQuantity(actualQty)
                        .damagedQuantity(damagedQty)
                        .unitCost(unitCost)
                        .totalCost(totalItemCost)
                        .variancePercentage(consVariancePct)
                        .varianceFlag(consFlag)
                        .build();

                run.addConsumption(consumption);
            }
        }

        // Add finished goods to stock
        inventoryService.recordProductMovement(
                product,
                actualGoodOutput,
                MovementType.PRODUCTION_RECEIPT,
                product.getSellingPrice(),
                ReferenceType.PRODUCTION_RUN,
                null,
                runNumber,
                "Finished goods receipt from run " + runNumber,
                currentUser
        );

        BigDecimal totalProductionCost = totalRmCost.add(totalPmCost).add(labourCost).add(overheadCost);
        BigDecimal costPerPacket = BigDecimal.ZERO;
        if (actualGoodOutput.compareTo(BigDecimal.ZERO) > 0) {
            costPerPacket = totalProductionCost.divide(actualGoodOutput, 4, RoundingMode.HALF_UP);
        }

        run.setRawMaterialCost(totalRmCost);
        run.setPackagingCost(totalPmCost);
        run.setTotalProductionCost(totalProductionCost);
        run.setCostPerPacket(costPerPacket);

        ProductionRun savedRun = productionRunRepository.save(run);
        return mapToDto(savedRun);
    }

    private void validateConsumption(BigDecimal actual, BigDecimal damaged, String itemName) {
        if (actual == null || damaged == null || actual.compareTo(BigDecimal.ZERO) < 0
                || damaged.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Consumption quantities for " + itemName + " cannot be negative");
        }
    }

    public ProductionRunResponseDto mapToDto(ProductionRun r) {
        List<MaterialConsumptionDto> consumptions = r.getConsumptions().stream().map(c -> {
            Long rmId = c.getRawMaterial() != null ? c.getRawMaterial().getId() : null;
            String rmName = c.getRawMaterial() != null ? c.getRawMaterial().getName() : null;
            String rmCode = c.getRawMaterial() != null ? c.getRawMaterial().getMaterialCode() : null;

            Long pmId = c.getPackagingMaterial() != null ? c.getPackagingMaterial().getId() : null;
            String pmName = c.getPackagingMaterial() != null ? c.getPackagingMaterial().getName() : null;
            String pmCode = c.getPackagingMaterial() != null ? c.getPackagingMaterial().getMaterialCode() : null;

            return MaterialConsumptionDto.builder()
                    .id(c.getId())
                    .itemType(c.getItemType())
                    .rawMaterialId(rmId)
                    .rawMaterialName(rmName)
                    .rawMaterialCode(rmCode)
                    .packagingMaterialId(pmId)
                    .packagingMaterialName(pmName)
                    .packagingMaterialCode(pmCode)
                    .plannedQuantity(c.getPlannedQuantity())
                    .actualQuantity(c.getActualQuantity())
                    .damagedQuantity(c.getDamagedQuantity())
                    .unitCost(c.getUnitCost())
                    .totalCost(c.getTotalCost())
                    .variancePercentage(c.getVariancePercentage())
                    .varianceFlag(c.isVarianceFlag())
                    .build();
        }).collect(Collectors.toList());

        return ProductionRunResponseDto.builder()
                .id(r.getId())
                .runNumber(r.getRunNumber())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .productCode(r.getProduct().getProductCode())
                .bomId(r.getBom().getId())
                .bomCode(r.getBom().getBomCode())
                .runDate(r.getRunDate())
                .batchMultiplier(r.getBatchMultiplier())
                .expectedOutput(r.getExpectedOutput())
                .actualGoodOutput(r.getActualGoodOutput())
                .yieldVariancePercentage(r.getYieldVariancePercentage())
                .varianceFlag(r.isVarianceFlag())
                .labourCost(r.getLabourCost())
                .overheadCost(r.getOverheadCost())
                .rawMaterialCost(r.getRawMaterialCost())
                .packagingCost(r.getPackagingCost())
                .totalProductionCost(r.getTotalProductionCost())
                .costPerPacket(r.getCostPerPacket())
                .status(r.getStatus())
                .supervisorName(r.getSupervisor() != null ? r.getSupervisor().getFullName() : "SYSTEM")
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .consumptions(consumptions)
                .build();
    }
}

