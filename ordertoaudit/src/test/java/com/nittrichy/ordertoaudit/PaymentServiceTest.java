package com.nittrichy.ordertoaudit;

import com.nittrichy.ordertoaudit.dto.PaymentRequestDto;
import com.nittrichy.ordertoaudit.entity.Customer;
import com.nittrichy.ordertoaudit.entity.Invoice;
import com.nittrichy.ordertoaudit.enums.InvoiceStatus;
import com.nittrichy.ordertoaudit.enums.PaymentMode;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.repository.CustomerRepository;
import com.nittrichy.ordertoaudit.repository.InvoiceRepository;
import com.nittrichy.ordertoaudit.repository.PaymentRepository;
import com.nittrichy.ordertoaudit.service.AuthService;
import com.nittrichy.ordertoaudit.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentRepository paymentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private AuthService authService;
    @InjectMocks private PaymentService paymentService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerCode("PAY-CUSTOMER")
                .name("Payment Test Customer")
                .state("Tamil Nadu")
                .outstandingBalance(new BigDecimal("100.00"))
                .build();
        customer.setId(7L);
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(customer));
        when(paymentRepository.findByPaymentNumber(any())).thenReturn(Optional.empty());
    }

    @Test
    void fullPaymentSettlesInvoiceAndCustomerBalance() {
        stubPaymentSave();
        Invoice invoice = invoice("100.00", "100.00");
        when(invoiceRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(invoice));

        paymentService.recordPayment(request("100.00", 11L, "PAY-FULL"));

        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getOutstandingAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(invoice.getPaidAmount()));
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(customer.getOutstandingBalance()));
    }

    @Test
    void partialPaymentPreservesRemainingBalances() {
        stubPaymentSave();
        Invoice invoice = invoice("100.00", "100.00");
        when(invoiceRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(invoice));

        paymentService.recordPayment(request("35.00", 11L, "PAY-PART"));

        assertEquals(0, new BigDecimal("65.00").compareTo(invoice.getOutstandingAmount()));
        assertEquals(0, new BigDecimal("35.00").compareTo(invoice.getPaidAmount()));
        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
        assertEquals(0, new BigDecimal("65.00").compareTo(customer.getOutstandingBalance()));
    }

    @Test
    void exactFifoSettlementAllocatesAcrossInvoicesWithoutRemainder() {
        stubPaymentSave();
        Invoice first = invoice("60.00", "60.00");
        Invoice second = invoice("40.00", "40.00");
        second.setId(12L);
        when(invoiceRepository.findByCustomerId(7L)).thenReturn(List.of(first, second));

        paymentService.recordPayment(request("100.00", null, "PAY-EXACT"));

        assertEquals(InvoiceStatus.PAID, first.getStatus());
        assertEquals(InvoiceStatus.PAID, second.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(first.getOutstandingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(second.getOutstandingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(customer.getOutstandingBalance()));
    }

    @Test
    void overpaymentIsRejectedWithoutChangingBalancesOrRecordingPayment() {
        Invoice invoice = invoice("100.00", "100.00");

        assertThrows(BusinessRuleException.class,
                () -> paymentService.recordPayment(request("100.01", 11L, "PAY-OVER")));

        assertEquals(0, new BigDecimal("100.00").compareTo(invoice.getOutstandingAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(customer.getOutstandingBalance()));
        verify(paymentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void duplicatePaymentNumberIsRejected() {
        when(paymentRepository.findByPaymentNumber("PAY-DUP")).thenReturn(Optional.of(new com.nittrichy.ordertoaudit.entity.Payment()));

        assertThrows(BusinessRuleException.class,
                () -> paymentService.recordPayment(request("10.00", null, "PAY-DUP")));
        verify(paymentRepository, never()).save(any());
    }

    private Invoice invoice(String amount, String outstanding) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-TEST-" + amount.replace('.', '-'))
                .customer(customer)
                .taxableValue(BigDecimal.ZERO)
                .grandTotal(new BigDecimal(amount))
                .invoiceDate(LocalDate.of(2026, 1, 1))
                .paidAmount(BigDecimal.ZERO)
                .outstandingAmount(new BigDecimal(outstanding))
                .status(InvoiceStatus.GENERATED)
                .build();
        invoice.setId(11L);
        return invoice;
    }

    private PaymentRequestDto request(String amount, Long invoiceId, String paymentNumber) {
        return PaymentRequestDto.builder()
                .paymentNumber(paymentNumber)
                .customerId(7L)
                .invoiceId(invoiceId)
                .amount(new BigDecimal(amount))
                .paymentMode(PaymentMode.CASH)
                .build();
    }

    private void stubPaymentSave() {
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
