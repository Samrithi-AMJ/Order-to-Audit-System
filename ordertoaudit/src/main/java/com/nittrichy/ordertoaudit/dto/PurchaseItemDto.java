package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItemDto {
    private Long id;

    @NotNull(message = "Item type is required")
    private ItemType itemType;

    private Long rawMaterialId;
    private String rawMaterialName;
    private String rawMaterialCode;

    private Long packagingMaterialId;
    private String packagingMaterialName;
    private String packagingMaterialCode;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;

    private BigDecimal totalAmount;
}

