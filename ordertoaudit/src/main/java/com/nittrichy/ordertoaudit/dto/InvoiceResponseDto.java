package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.InvoiceStatus;
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
public class InvoiceResponseDto {
    private Long id;
    private String invoiceNumber;
    private Long salesOrderId;
    private String salesOrderNumber;
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerGstin;
    private String customerAddress;
    private String placeOfSupply;
    private boolean isInterState;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal taxableValue;
    private BigDecimal cgstRate;
    private BigDecimal cgstAmount;
    private BigDecimal sgstRate;
    private BigDecimal sgstAmount;
    private BigDecimal igstRate;
    private BigDecimal igstAmount;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private InvoiceStatus status;
    private String generatedByName;
    private String notes;
    private LocalDateTime createdAt;
    private List<InvoiceItemDto> items;
}

