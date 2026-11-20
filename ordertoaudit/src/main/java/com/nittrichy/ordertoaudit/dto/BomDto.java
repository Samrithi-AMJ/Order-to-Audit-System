package com.nittrichy.ordertoaudit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomDto {
    private Long id;

    @NotBlank(message = "BOM code is required")
    private String bomCode;

    @NotNull(message = "Product ID is required")
    private Long productId;
    private String productName;
    private String productCode;

    @NotNull(message = "Batch size is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Batch size must be greater than zero")
    private BigDecimal batchSize; // e.g. 200 packets

    private String description;
    private Integer version;
    private boolean active;

    @NotEmpty(message = "BOM must contain at least one item")
    @Valid
    private List<BomItemDto> items;
}

