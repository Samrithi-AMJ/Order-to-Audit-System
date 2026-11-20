package com.nittrichy.ordertoaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private BigDecimal totalSales;
    private BigDecimal totalReceivables;
    private BigDecimal totalPurchases;
    private Long pendingOrdersCount;
    private Long activeProductionRunsCount;
    private Long lowStockAlertsCount;
    private BigDecimal rawMaterialStockValuation;
    private BigDecimal packagingStockValuation;
    private BigDecimal finishedGoodsStockValuation;
    private Map<String, BigDecimal> categoryWiseSales;
}

