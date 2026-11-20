package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDto {
    private Long id;
    private String movementCode;
    private LocalDateTime movementDate;
    private MovementType movementType;
    private ItemType itemType;

    private Long itemId;
    private String itemCode;
    private String itemName;
    private String unit;

    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal openingStock;
    private BigDecimal closingStock;

    private ReferenceType referenceType;
    private Long referenceId;
    private String referenceNumber;
    private String remarks;
    private String createdByName;
}

