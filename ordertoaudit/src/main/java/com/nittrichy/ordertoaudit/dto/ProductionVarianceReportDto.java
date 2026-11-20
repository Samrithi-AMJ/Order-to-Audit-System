package com.nittrichy.ordertoaudit.dto;

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
public class ProductionVarianceReportDto {
    private String runNumber;
    private String productName;
    private String runDate;
    private BigDecimal expectedOutput;
    private BigDecimal actualGoodOutput;
    private BigDecimal yieldVariancePercentage;
    private boolean yieldVarianceFlagged;
    private BigDecimal totalCost;
    private BigDecimal costPerPacket;
    private List<MaterialVarianceItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialVarianceItemDto {
        private String materialName;
        private String materialType;
        private BigDecimal plannedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal damagedQuantity;
        private BigDecimal variancePercentage;
        private boolean flagged;
        private BigDecimal totalCost;
    }
}

