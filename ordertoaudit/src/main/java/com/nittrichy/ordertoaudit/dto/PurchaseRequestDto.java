package com.nittrichy.ordertoaudit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDto {
    private String purchaseNumber;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private LocalDate purchaseDate;
    private String supplierInvoiceNo;
    private String notes;

    @NotEmpty(message = "Purchase must have at least one item")
    @Valid
    private List<PurchaseItemDto> items;
}

