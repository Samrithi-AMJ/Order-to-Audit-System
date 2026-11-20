package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.MaterialCategory;
import jakarta.validation.constraints.NotBlank;
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
public class RawMaterialDto {
    private Long id;

    @NotBlank(message = "Material code is required")
    private String materialCode;

    @NotBlank(message = "Material name is required")
    private String name;

    @NotNull(message = "Category is required")
    private MaterialCategory category;

    @NotBlank(message = "Unit is required")
    private String unit;

    private BigDecimal currentStock;
    private BigDecimal weightedAvgCost;
    private BigDecimal reorderLevel;
    private boolean active;
}

