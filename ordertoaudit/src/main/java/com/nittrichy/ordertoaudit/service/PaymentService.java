package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.PaymentRequestDto;
import com.nittrichy.ordertoaudit.dto.PaymentResponseDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.InvoiceStatus;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.CustomerRepository;
import com.nittrichy.ordertoaudit.repository.InvoiceRepository;
import com.nittrichy.ordertoaudit.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponseDto recordPayment(PaymentRequestDto request) {
        Customer customer = customerRepository.findByIdForUpdate(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero");
        }

        User currentUser = authService.getCurrentAuthenticatedUser();

        String paymentNumber = request.getPaymentNumber();
        if (paymentNumber == null || paymentNumber.isBlank()) {
            paymentNumber = "PAY-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
        if (paymentRepository.findByPaymentNumber(paymentNumber).isPresent()) {
            throw new BusinessRuleException("Payment number '" + paymentNumber + "' has already been recorded");
        }

        BigDecimal currentBal = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
        if (amount.compareTo(currentBal) > 0) {
            throw new BusinessRuleException("Payment amount exceeds the customer's outstanding balance");
        }

        Invoice targetInvoice = null;

        if (request.getInvoiceId() != null) {
            targetInvoice = invoiceRepository.findByIdForUpdate(request.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

            if (!targetInvoice.getCustomer().getId().equals(customer.getId())) {
                throw new BusinessRuleException("Invoice does not belong to specified customer");
            }

            if (targetInvoice.getStatus() == InvoiceStatus.PAID || targetInvoice.getStatus() == InvoiceStatus.CANCELLED) {
                throw new BusinessRuleException("Invoice " + targetInvoice.getInvoiceNumber() + " cannot accept payments in status " + targetInvoice.getStatus());
            }

            if (amount.compareTo(targetInvoice.getOutstandingAmount()) > 0) {
                throw new BusinessRuleException(String.format("Payment amount (₹%s) exceeds outstanding invoice balance (₹%s)",
                        amount, targetInvoice.getOutstandingAmount()));
            }

            BigDecimal newPaidAmount = targetInvoice.getPaidAmount().add(amount);
            BigDecimal newOutstanding = targetInvoice.getOutstandingAmount().subtract(amount);

            targetInvoice.setPaidAmount(newPaidAmount);
            targetInvoice.setOutstandingAmount(newOutstanding);

            if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                targetInvoice.setStatus(InvoiceStatus.PAID);
            } else {
                targetInvoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }

            invoiceRepository.save(targetInvoice);

        } else {
            // FIFO allocation over customer's unpaid invoices
            List<Invoice> unpaidInvoices = invoiceRepository.findByCustomerId(customer.getId()).stream()
                    .filter(inv -> inv.getStatus() != InvoiceStatus.PAID && inv.getStatus() != InvoiceStatus.CANCELLED)
                    .sorted((a, b) -> a.getInvoiceDate().compareTo(b.getInvoiceDate()))
                    .collect(Collectors.toList());

            BigDecimal allocatable = unpaidInvoices.stream()
                    .map(Invoice::getOutstandingAmount)
                    .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (amount.compareTo(allocatable) > 0) {
                throw new BusinessRuleException("Payment amount exceeds outstanding invoice balances; advance payments are not supported");
            }

            BigDecimal remaining = amount;
            for (Invoice inv : unpaidInvoices) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal toPay = remaining.min(inv.getOutstandingAmount());
                inv.setPaidAmount(inv.getPaidAmount().add(toPay));
                inv.setOutstandingAmount(inv.getOutstandingAmount().subtract(toPay));
                if (inv.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    inv.setStatus(InvoiceStatus.PAID);
                } else {
                    inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
                }
                invoiceRepository.save(inv);
                remaining = remaining.subtract(toPay);
                if (targetInvoice == null) targetInvoice = inv;
            }
        }

        // Deduct from customer total outstanding balance
        BigDecimal newBal = currentBal.subtract(amount);
        customer.setOutstandingBalance(newBal);
        customerRepository.save(customer);

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .customer(customer)
                .invoice(targetInvoice)
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .amount(amount)
                .paymentMode(request.getPaymentMode())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .recordedBy(currentUser)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return mapToDto(savedPayment);
    }

    public PaymentResponseDto mapToDto(Payment p) {
        BigDecimal remainingInvoiceBal = p.getInvoice() != null ? p.getInvoice().getOutstandingAmount() : null;
        BigDecimal remainingCustBal = p.getCustomer() != null ? p.getCustomer().getOutstandingBalance() : null;

        return PaymentResponseDto.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getName())
                .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                .invoiceNumber(p.getInvoice() != null ? p.getInvoice().getInvoiceNumber() : null)
                .paymentDate(p.getPaymentDate())
                .amount(p.getAmount())
                .paymentMode(p.getPaymentMode())
                .referenceNumber(p.getReferenceNumber())
                .notes(p.getNotes())
                .recordedByName(p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : "SYSTEM")
                .createdAt(p.getCreatedAt())
                .remainingCustomerBalance(remainingCustBal)
                .remainingInvoiceBalance(remainingInvoiceBal)
                .build();
    }
}

