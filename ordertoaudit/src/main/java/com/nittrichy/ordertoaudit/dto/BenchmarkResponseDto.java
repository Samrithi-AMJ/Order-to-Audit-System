package com.nittrichy.ordertoaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResponseDto {
    private boolean success;
    private String message;
    private BenchmarkStockResult stockBalances;
    private BenchmarkVarianceResult varianceResults;
    private BenchmarkSalesResult retailerASalesResult;
    private BenchmarkDistributorBResult distributorBResult;
    private BenchmarkCostingResult costingResult;
    private List<BenchmarkCheck> auditVerifications;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkStockResult {
        private BigDecimal flourClosingStock;      // Expected: 120.000 kg
        private BigDecimal oilClosingStock;        // Expected: 19.500 litres
        private BigDecimal spiceClosingStock;      // Expected: 3.800 kg
        private BigDecimal pouchClosingStock;      // Expected: 306.000 pieces
        private BigDecimal cartonClosingStock;     // Expected: 21.000 pieces
        private BigDecimal finishedGoodsPostProd;  // Expected: 238.000 packets
        private BigDecimal finishedGoodsPostSale;  // Expected: 88.000 packets
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkVarianceResult {
        private BigDecimal yieldVariancePercentage; // Expected: -6.00%
        private BigDecimal oilConsumptionVariance;  // Expected: +5.00%
        private boolean yieldVarianceFlagged;       // Expected: true (>3%)
        private boolean oilVarianceFlagged;         // Expected: true (>3%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkSalesResult {
        private String customerName;              // Retailer A (Tamil Nadu)
        private BigDecimal quantity;              // 150 packets
        private BigDecimal unitPrice;             // ₹50.00
        private BigDecimal taxableValue;          // ₹7,500.00
        private BigDecimal cgstAmount;            // ₹187.50 (2.5%)
        private BigDecimal sgstAmount;            // ₹187.50 (2.5%)
        private BigDecimal igstAmount;            // ₹0.00
        private BigDecimal invoiceTotal;          // ₹7,875.00
        private BigDecimal paymentReceived;       // ₹4,000.00
        private BigDecimal outstandingAmount;     // ₹3,875.00
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkDistributorBResult {
        private String customerName;              // Distributor B (Karnataka)
        private BigDecimal orderedQuantity;       // 120 packets
        private BigDecimal availableQuantity;     // 88 packets
        private BigDecimal shortageQuantity;      // 32 packets
        private boolean dispatchBlocked;          // true (Insufficient stock)
        private String blockReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkCostingResult {
        private BigDecimal rawMaterialCost;
        private BigDecimal packagingCost;
        private BigDecimal labourCost;            // ₹1,200.00
        private BigDecimal overheadCost;          // ₹600.00
        private BigDecimal totalProductionCost;
        private BigDecimal costPerGoodPacket;     // Total cost / 188 packets
        private BigDecimal sellingPrice;          // ₹50.00
        private BigDecimal grossMarginPerPacket;
        private BigDecimal grossMarginPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkCheck {
        private String checkName;
        private String expected;
        private String actual;
        private boolean passed;
    }
}

