package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.StockMovementDto;
import com.nittrichy.ordertoaudit.dto.StockSummaryDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;
    private final ProductRepository productRepository;

    @Transactional
    public StockMovement recordRawMaterialMovement(
            RawMaterial material,
            BigDecimal quantity, // positive for addition, negative for deduction
            MovementType movementType,
            BigDecimal unitPrice,
            ReferenceType referenceType,
            Long referenceId,
            String referenceNumber,
            String remarks,
            User createdBy) {

        validateMovementQuantity(quantity);
        Long materialId = material.getId();
        material = rawMaterialRepository.findByIdForUpdate(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("RawMaterial", "id", materialId));

        BigDecimal openingStock = material.getCurrentStock() != null ? material.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal closingStock = openingStock.add(quantity);

        if (closingStock.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Insufficient raw material stock for {}. Requested reduction: {}, Available: {}",
                    material.getName(), quantity.abs(), openingStock);
            throw new InsufficientStockException(
                    material.getMaterialCode(),
                    material.getName(),
                    quantity.abs(),
                    openingStock
            );
        }

        material.setCurrentStock(closingStock);
        rawMaterialRepository.save(material);

        StockMovement movement = StockMovement.builder()
                .movementCode("SM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .movementDate(LocalDateTime.now())
                .movementType(movementType)
                .itemType(ItemType.RAW_MATERIAL)
                .rawMaterial(material)
                .quantity(quantity)
                .unitPrice(unitPrice != null ? unitPrice : material.getWeightedAvgCost())
                .openingStock(openingStock)
                .closingStock(closingStock)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .remarks(remarks)
                .createdBy(createdBy)
                .build();

        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovement recordPackagingMaterialMovement(
            PackagingMaterial material,
            BigDecimal quantity, // positive for addition, negative for deduction
            MovementType movementType,
            BigDecimal unitPrice,
            ReferenceType referenceType,
            Long referenceId,
            String referenceNumber,
            String remarks,
            User createdBy) {

        validateMovementQuantity(quantity);
        Long materialId = material.getId();
        material = packagingMaterialRepository.findByIdForUpdate(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("PackagingMaterial", "id", materialId));

        BigDecimal openingStock = material.getCurrentStock() != null ? material.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal closingStock = openingStock.add(quantity);

        if (closingStock.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Insufficient packaging material stock for {}. Requested reduction: {}, Available: {}",
                    material.getName(), quantity.abs(), openingStock);
            throw new InsufficientStockException(
                    material.getMaterialCode(),
                    material.getName(),
                    quantity.abs(),
                    openingStock
            );
        }

        material.setCurrentStock(closingStock);
        packagingMaterialRepository.save(material);

        StockMovement movement = StockMovement.builder()
                .movementCode("SM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .movementDate(LocalDateTime.now())
                .movementType(movementType)
                .itemType(ItemType.PACKAGING_MATERIAL)
                .packagingMaterial(material)
                .quantity(quantity)
                .unitPrice(unitPrice != null ? unitPrice : material.getUnitCost())
                .openingStock(openingStock)
                .closingStock(closingStock)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .remarks(remarks)
                .createdBy(createdBy)
                .build();

        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovement recordProductMovement(
            Product product,
            BigDecimal quantity, // positive for addition, negative for deduction
            MovementType movementType,
            BigDecimal unitPrice,
            ReferenceType referenceType,
            Long referenceId,
            String referenceNumber,
            String remarks,
            User createdBy) {

        validateMovementQuantity(quantity);
        Long productId = product.getId();
        product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        BigDecimal openingStock = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal closingStock = openingStock.add(quantity);

        if (closingStock.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Insufficient finished goods stock for {}. Requested reduction: {}, Available: {}",
                    product.getName(), quantity.abs(), openingStock);
            throw new InsufficientStockException(
                    product.getProductCode(),
                    product.getName(),
                    quantity.abs(),
                    openingStock
            );
        }

        product.setCurrentStock(closingStock);
        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .movementCode("SM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .movementDate(LocalDateTime.now())
                .movementType(movementType)
                .itemType(ItemType.FINISHED_GOODS)
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice != null ? unitPrice : product.getSellingPrice())
                .openingStock(openingStock)
                .closingStock(closingStock)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .remarks(remarks)
                .createdBy(createdBy)
                .build();

        return stockMovementRepository.save(movement);
    }

    private void validateMovementQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("Stock movement quantity must be non-zero");
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getStockLedger(LocalDateTime start, LocalDateTime end, ItemType itemType) {
        List<StockMovement> movements;
        if (start != null && end != null) {
            movements = stockMovementRepository.findByMovementDateBetweenOrderByMovementDateDesc(start, end);
        } else if (itemType != null) {
            movements = stockMovementRepository.findByItemTypeOrderByMovementDateDesc(itemType);
        } else {
            movements = stockMovementRepository.findAll().stream()
                    .sorted((a, b) -> b.getMovementDate().compareTo(a.getMovementDate()))
                    .collect(Collectors.toList());
        }

        return movements.stream().map(this::mapMovementToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockSummaryDto> getAllStockSummaries() {
        List<StockSummaryDto> list = new ArrayList<>();

        for (RawMaterial rm : rawMaterialRepository.findAll()) {
            BigDecimal stock = rm.getCurrentStock() != null ? rm.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal rate = rm.getWeightedAvgCost() != null ? rm.getWeightedAvgCost() : BigDecimal.ZERO;
            list.add(StockSummaryDto.builder()
                    .id(rm.getId())
                    .itemType(ItemType.RAW_MATERIAL)
                    .itemCode(rm.getMaterialCode())
                    .itemName(rm.getName())
                    .categoryOrType(rm.getCategory().name())
                    .unit(rm.getUnit())
                    .currentStock(stock)
                    .valuationRate(rate)
                    .totalValuation(stock.multiply(rate))
                    .reorderLevel(rm.getReorderLevel())
                    .lowStock(rm.getReorderLevel() != null && stock.compareTo(rm.getReorderLevel()) <= 0)
                    .build());
        }

        for (PackagingMaterial pm : packagingMaterialRepository.findAll()) {
            BigDecimal stock = pm.getCurrentStock() != null ? pm.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal rate = pm.getUnitCost() != null ? pm.getUnitCost() : BigDecimal.ZERO;
            list.add(StockSummaryDto.builder()
                    .id(pm.getId())
                    .itemType(ItemType.PACKAGING_MATERIAL)
                    .itemCode(pm.getMaterialCode())
                    .itemName(pm.getName())
                    .categoryOrType(pm.getType().name())
                    .unit(pm.getUnit())
                    .currentStock(stock)
                    .valuationRate(rate)
                    .totalValuation(stock.multiply(rate))
                    .reorderLevel(pm.getReorderLevel())
                    .lowStock(pm.getReorderLevel() != null && stock.compareTo(pm.getReorderLevel()) <= 0)
                    .build());
        }

        for (Product p : productRepository.findAll()) {
            BigDecimal stock = p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal rate = p.getSellingPrice() != null ? p.getSellingPrice() : BigDecimal.ZERO;
            list.add(StockSummaryDto.builder()
                    .id(p.getId())
                    .itemType(ItemType.FINISHED_GOODS)
                    .itemCode(p.getProductCode())
                    .itemName(p.getName())
                    .categoryOrType(p.getCategory().name())
                    .unit(p.getUnit())
                    .currentStock(stock)
                    .valuationRate(rate)
                    .totalValuation(stock.multiply(rate))
                    .reorderLevel(p.getReorderLevel())
                    .lowStock(p.getReorderLevel() != null && stock.compareTo(p.getReorderLevel()) <= 0)
                    .build());
        }

        return list;
    }

    public StockMovementDto mapMovementToDto(StockMovement sm) {
        Long itemId = null;
        String itemCode = null;
        String itemName = null;
        String unit = null;

        if (sm.getRawMaterial() != null) {
            itemId = sm.getRawMaterial().getId();
            itemCode = sm.getRawMaterial().getMaterialCode();
            itemName = sm.getRawMaterial().getName();
            unit = sm.getRawMaterial().getUnit();
        } else if (sm.getPackagingMaterial() != null) {
            itemId = sm.getPackagingMaterial().getId();
            itemCode = sm.getPackagingMaterial().getMaterialCode();
            itemName = sm.getPackagingMaterial().getName();
            unit = sm.getPackagingMaterial().getUnit();
        } else if (sm.getProduct() != null) {
            itemId = sm.getProduct().getId();
            itemCode = sm.getProduct().getProductCode();
            itemName = sm.getProduct().getName();
            unit = sm.getProduct().getUnit();
        }

        return StockMovementDto.builder()
                .id(sm.getId())
                .movementCode(sm.getMovementCode())
                .movementDate(sm.getMovementDate())
                .movementType(sm.getMovementType())
                .itemType(sm.getItemType())
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .unit(unit)
                .quantity(sm.getQuantity())
                .unitPrice(sm.getUnitPrice())
                .openingStock(sm.getOpeningStock())
                .closingStock(sm.getClosingStock())
                .referenceType(sm.getReferenceType())
                .referenceId(sm.getReferenceId())
                .referenceNumber(sm.getReferenceNumber())
                .remarks(sm.getRemarks())
                .createdByName(sm.getCreatedBy() != null ? sm.getCreatedBy().getFullName() : "SYSTEM")
                .build();
    }
}

