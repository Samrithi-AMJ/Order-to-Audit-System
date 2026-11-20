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
public class GstSummaryReportDto {
    private String financialYear;
    private BigDecimal totalTaxableValue;
    private BigDecimal totalCgst;
    private BigDecimal totalSgst;
    private BigDecimal totalIgst;
    private BigDecimal totalGstCollected;
    private BigDecimal totalGrossInvoiced;
    private List<GstLineItemDto> lineItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstLineItemDto {
        private String invoiceNumber;
        private String invoiceDate;
        private String customerName;
        private String customerGstin;
        private String placeOfSupply;
        private boolean isInterState;
        private BigDecimal taxableValue;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal invoiceTotal;
    }
}

