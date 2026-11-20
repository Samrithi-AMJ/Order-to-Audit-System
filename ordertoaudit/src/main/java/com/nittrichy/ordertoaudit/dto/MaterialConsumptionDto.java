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
public class MaterialConsumptionDto {
    private Long id;

    @NotNull(message = "Item type is required")
    private ItemType itemType;

    private Long rawMaterialId;
    private String rawMaterialName;
    private String rawMaterialCode;

    private Long packagingMaterialId;
    private String packagingMaterialName;
    private String packagingMaterialCode;

    private BigDecimal plannedQuantity;

    @NotNull(message = "Actual quantity is required")
    @DecimalMin(value = "0.0", message = "Actual quantity cannot be negative")
    private BigDecimal actualQuantity;

    private BigDecimal damagedQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private BigDecimal variancePercentage;
    private boolean varianceFlag; // > 3%
}

