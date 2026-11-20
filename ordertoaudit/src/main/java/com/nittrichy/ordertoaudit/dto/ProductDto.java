package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
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
public class ProductDto {
    private Long id;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Category is required")
    private ProductCategory category;

    @NotBlank(message = "Unit is required")
    private String unit;

    private Integer packetWeightGrams;

    private String hsnCode;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;

    @NotNull(message = "GST rate is required")
    private BigDecimal gstRate;

    private BigDecimal currentStock;
    private BigDecimal reorderLevel;
    private boolean active;
}

