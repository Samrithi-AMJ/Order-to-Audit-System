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
public class BomItemDto {
    private Long id;

    @NotNull(message = "Item type is required (RAW_MATERIAL or PACKAGING_MATERIAL)")
    private ItemType itemType;

    private Long rawMaterialId;
    private String rawMaterialName;
    private String rawMaterialCode;

    private Long packagingMaterialId;
    private String packagingMaterialName;
    private String packagingMaterialCode;

    @NotNull(message = "Standard quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Standard quantity must be greater than zero")
    private BigDecimal standardQuantity;

    private String unit;
    private String notes;
}

