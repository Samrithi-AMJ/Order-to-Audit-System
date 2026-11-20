package com.nittrichy.ordertoaudit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRunRequestDto {
    private String runNumber;

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long bomId; // Optional; uses active BOM if omitted

    @Builder.Default
    private BigDecimal batchMultiplier = BigDecimal.ONE;

    @NotNull(message = "Actual good output is required")
    @DecimalMin(value = "0.0", message = "Actual good output cannot be negative")
    private BigDecimal actualGoodOutput; // e.g. 188

    private BigDecimal labourCost;   // e.g. 1200.00
    private BigDecimal overheadCost; // e.g. 600.00 (gas, power)

    private LocalDate runDate;
    private String notes;

    @Valid
    private List<MaterialConsumptionDto> consumptions;
}

