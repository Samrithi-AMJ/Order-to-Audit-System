package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.OrderStatus;
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
public class SalesOrderResponseDto {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerState;
    private String customerGstin;
    private LocalDate orderDate;
    private String salesmanName;
    private OrderStatus status;
    private BigDecimal totalTaxableAmount;
    private BigDecimal totalGstAmount;
    private BigDecimal grandTotal;
    private String rejectionReason;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime dispatchedAt;
    private String notes;
    private LocalDateTime createdAt;
    private List<SalesOrderItemDto> items;
}

