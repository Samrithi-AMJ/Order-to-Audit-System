package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryDto {
    private Long id;
    private ItemType itemType;
    private String itemCode;
    private String itemName;
    private String categoryOrType;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal valuationRate;
    private BigDecimal totalValuation;
    private BigDecimal reorderLevel;
    private boolean lowStock;
}

