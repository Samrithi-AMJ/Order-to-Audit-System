package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.PurchaseItemDto;
import com.nittrichy.ordertoaudit.dto.PurchaseRequestDto;
import com.nittrichy.ordertoaudit.dto.PurchaseResponseDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.PurchaseStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;
    private final InventoryService inventoryService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<PurchaseResponseDto> getAllPurchases() {
        return purchaseRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseResponseDto getPurchaseById(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));
        return mapToDto(purchase);
    }

    @Transactional
    public PurchaseResponseDto recordPurchase(PurchaseRequestDto request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        String purchaseNumber = request.getPurchaseNumber();
        if (purchaseNumber == null || purchaseNumber.isBlank()) {
            purchaseNumber = "PO-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        User currentUser = authService.getCurrentAuthenticatedUser();

        Purchase purchase = Purchase.builder()
                .purchaseNumber(purchaseNumber)
                .supplier(supplier)
                .purchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDate.now())
                .status(PurchaseStatus.RECEIVED)
                .supplierInvoiceNo(request.getSupplierInvoiceNo())
                .notes(request.getNotes())
                .recordedBy(currentUser)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (PurchaseItemDto itemDto : request.getItems()) {
            BigDecimal qty = itemDto.getQuantity();
            BigDecimal price = itemDto.getUnitPrice();
            BigDecimal itemTotal = qty.multiply(price);
            grandTotal = grandTotal.add(itemTotal);

            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .itemType(itemDto.getItemType())
                    .quantity(qty)
                    .unitPrice(price)
                    .totalAmount(itemTotal)
                    .build();

            if (itemDto.getItemType() == ItemType.RAW_MATERIAL) {
                RawMaterial rm = rawMaterialRepository.findByIdForUpdate(itemDto.getRawMaterialId())
                        .orElseThrow(() -> new ResourceNotFoundException("RawMaterial", "id", itemDto.getRawMaterialId()));

                purchaseItem.setRawMaterial(rm);

                // Update weighted-average cost for raw material
                BigDecimal currentStock = rm.getCurrentStock() != null ? rm.getCurrentStock() : BigDecimal.ZERO;
                BigDecimal currentCost = rm.getWeightedAvgCost() != null ? rm.getWeightedAvgCost() : BigDecimal.ZERO;
                BigDecimal currentTotalValuation = currentStock.multiply(currentCost);
                BigDecimal newIncomingValuation = qty.multiply(price);
                BigDecimal newTotalStock = currentStock.add(qty);

                if (newTotalStock.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newAvgCost = currentTotalValuation.add(newIncomingValuation)
                            .divide(newTotalStock, 4, RoundingMode.HALF_UP);
                    rm.setWeightedAvgCost(newAvgCost);
                    rawMaterialRepository.save(rm);
                }

                // Record stock movement (ledger)
                inventoryService.recordRawMaterialMovement(
                        rm,
                        qty,
                        MovementType.PURCHASE_RECEIPT,
                        price,
                        ReferenceType.PURCHASE,
                        null,
                        purchaseNumber,
                        "Goods receipt from " + supplier.getName(),
                        currentUser
                );

            } else if (itemDto.getItemType() == ItemType.PACKAGING_MATERIAL) {
                PackagingMaterial pm = packagingMaterialRepository.findByIdForUpdate(itemDto.getPackagingMaterialId())
                        .orElseThrow(() -> new ResourceNotFoundException("PackagingMaterial", "id", itemDto.getPackagingMaterialId()));

                purchaseItem.setPackagingMaterial(pm);
                pm.setUnitCost(price);
                packagingMaterialRepository.save(pm);

                // Record stock movement (ledger)
                inventoryService.recordPackagingMaterialMovement(
                        pm,
                        qty,
                        MovementType.PURCHASE_RECEIPT,
                        price,
                        ReferenceType.PURCHASE,
                        null,
                        purchaseNumber,
                        "Packaging receipt from " + supplier.getName(),
                        currentUser
                );
            }

            purchase.addItem(purchaseItem);
        }

        purchase.setTotalAmount(grandTotal);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        return mapToDto(savedPurchase);
    }

    public PurchaseResponseDto mapToDto(Purchase p) {
        List<PurchaseItemDto> items = p.getItems().stream().map(item -> {
            Long rmId = item.getRawMaterial() != null ? item.getRawMaterial().getId() : null;
            String rmName = item.getRawMaterial() != null ? item.getRawMaterial().getName() : null;
            String rmCode = item.getRawMaterial() != null ? item.getRawMaterial().getMaterialCode() : null;

            Long pmId = item.getPackagingMaterial() != null ? item.getPackagingMaterial().getId() : null;
            String pmName = item.getPackagingMaterial() != null ? item.getPackagingMaterial().getName() : null;
            String pmCode = item.getPackagingMaterial() != null ? item.getPackagingMaterial().getMaterialCode() : null;

            return PurchaseItemDto.builder()
                    .id(item.getId())
                    .itemType(item.getItemType())
                    .rawMaterialId(rmId)
                    .rawMaterialName(rmName)
                    .rawMaterialCode(rmCode)
                    .packagingMaterialId(pmId)
                    .packagingMaterialName(pmName)
                    .packagingMaterialCode(pmCode)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalAmount(item.getTotalAmount())
                    .build();
        }).collect(Collectors.toList());

        return PurchaseResponseDto.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(p.getSupplier().getId())
                .supplierName(p.getSupplier().getName())
                .supplierCode(p.getSupplier().getSupplierCode())
                .purchaseDate(p.getPurchaseDate())
                .status(p.getStatus())
                .supplierInvoiceNo(p.getSupplierInvoiceNo())
                .totalAmount(p.getTotalAmount())
                .notes(p.getNotes())
                .recordedByName(p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : "SYSTEM")
                .createdAt(p.getCreatedAt())
                .items(items)
                .build();
    }
}

