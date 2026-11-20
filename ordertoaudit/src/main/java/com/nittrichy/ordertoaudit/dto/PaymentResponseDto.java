package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private String paymentNumber;
    private Long customerId;
    private String customerName;
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String referenceNumber;
    private String notes;
    private String recordedByName;
    private LocalDateTime createdAt;
    private BigDecimal remainingCustomerBalance;
    private BigDecimal remainingInvoiceBalance;
}

