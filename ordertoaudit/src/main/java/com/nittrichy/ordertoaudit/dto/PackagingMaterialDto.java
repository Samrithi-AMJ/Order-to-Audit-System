package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.PackagingType;
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
public class PackagingMaterialDto {
    private Long id;

    @NotBlank(message = "Material code is required")
    private String materialCode;

    @NotBlank(message = "Material name is required")
    private String name;

    @NotNull(message = "Packaging type is required")
    private PackagingType type;

    @NotBlank(message = "Unit is required")
    private String unit;

    private BigDecimal currentStock;
    private BigDecimal unitCost;
    private BigDecimal reorderLevel;
    private boolean active;
}

