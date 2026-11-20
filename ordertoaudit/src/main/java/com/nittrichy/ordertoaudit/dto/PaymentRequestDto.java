package com.nittrichy.ordertoaudit.dto;

import com.nittrichy.ordertoaudit.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private String paymentNumber;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private Long invoiceId; // Optional; if provided, applies directly to this invoice

    private LocalDate paymentDate;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String referenceNumber; // e.g. UPI Ref / Cheque No
    private String notes;
}

