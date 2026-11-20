package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.ProductionStatus;
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
public class ProductionRunResponseDto {
    private Long id;
    private String runNumber;
    private Long productId;
    private String productName;
    private String productCode;
    private Long bomId;
    private String bomCode;
    private LocalDate runDate;
    private BigDecimal batchMultiplier;
    private BigDecimal expectedOutput;
    private BigDecimal actualGoodOutput;
    private BigDecimal yieldVariancePercentage;
    private boolean varianceFlag;
    private BigDecimal labourCost;
    private BigDecimal overheadCost;
    private BigDecimal rawMaterialCost;
    private BigDecimal packagingCost;
    private BigDecimal totalProductionCost;
    private BigDecimal costPerPacket;
    private ProductionStatus status;
    private String supervisorName;
    private String notes;
    private LocalDateTime createdAt;
    private List<MaterialConsumptionDto> consumptions;
}

