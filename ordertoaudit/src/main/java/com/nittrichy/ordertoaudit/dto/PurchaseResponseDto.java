package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponseDto {
    private Long id;
    private String purchaseNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private LocalDate purchaseDate;
    private PurchaseStatus status;
    private String supplierInvoiceNo;
    private BigDecimal totalAmount;
    private String notes;
    private String recordedByName;
    private LocalDateTime createdAt;
    private List<PurchaseItemDto> items;
}

