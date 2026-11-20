package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.InvoiceItemDto;
import com.nittrichy.ordertoaudit.dto.InvoiceResponseDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.InvoiceStatus;
import com.nittrichy.ordertoaudit.enums.OrderStatus;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.CustomerRepository;
import com.nittrichy.ordertoaudit.repository.InvoiceRepository;
import com.nittrichy.ordertoaudit.repository.SalesOrderRepository;
import com.nittrichy.ordertoaudit.util.PdfInvoiceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;
    private final AuthService authService;

    @Value("${app.company.state:Tamil Nadu}")
    private String homeState;

    @Value("${app.company.state-code:33}")
    private String homeStateCode;

    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getAllInvoices() {
        return invoiceRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return mapToDto(invoice);
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceResponseDto> getInvoiceBySalesOrderId(Long salesOrderId) {
        return invoiceRepository.findBySalesOrderId(salesOrderId).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
        try {
            return pdfInvoiceGenerator.generateInvoicePdf(invoice);
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}", invoice.getInvoiceNumber(), e);
            throw new BusinessRuleException("Could not generate invoice PDF: " + e.getMessage());
        }
    }

    @Transactional
    public Invoice generateInvoiceForOrder(Long salesOrderId) {
        Optional<Invoice> existingInvoice = invoiceRepository.findBySalesOrderId(salesOrderId);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        SalesOrder order = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", salesOrderId));

        if (order.getStatus() == OrderStatus.REJECTED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot generate invoice for order in " + order.getStatus() + " status");
        }

        Customer customer = customerRepository.findByIdForUpdate(order.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", order.getCustomer().getId()));
        User currentUser = authService.getCurrentAuthenticatedUser();

        LocalDate today = LocalDate.now();
        String financialYearPrefix = getFinancialYearPrefix(today);
        long count = invoiceRepository.countByInvoiceNumberPrefix(financialYearPrefix);
        String invoiceNumber = String.format("%s-%04d", financialYearPrefix, count + 1);

        boolean isInterState = customer.getStateCode() != null && !customer.getStateCode().isBlank()
                ? !homeStateCode.equalsIgnoreCase(customer.getStateCode().trim())
                : !homeState.equalsIgnoreCase(customer.getState().trim());

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .salesOrder(order)
                .customer(customer)
                .invoiceDate(today)
                .dueDate(today.plusDays(15))
                .customerGstin(customer.getGstin())
                .placeOfSupply(customer.getState())
                .isInterState(isInterState)
                .taxableValue(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .outstandingAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.GENERATED)
                .generatedBy(currentUser)
                .notes("Generated for Sales Order " + order.getOrderNumber())
                .build();

        for (SalesOrderItem item : order.getItems()) {
            BigDecimal qty = item.getQuantity();
            BigDecimal rate = item.getUnitPrice();
            BigDecimal taxable = qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            totalTaxable = totalTaxable.add(taxable);
            BigDecimal gstRate = item.getGstRate() != null ? item.getGstRate() : item.getProduct().getGstRate();
            if (gstRate == null || gstRate.compareTo(BigDecimal.ZERO) < 0 || gstRate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRuleException("Product " + item.getProduct().getProductCode() + " has an invalid GST rate");
            }

            BigDecimal cgst = BigDecimal.ZERO;
            BigDecimal sgst = BigDecimal.ZERO;
            BigDecimal igst = BigDecimal.ZERO;

            if (isInterState) {
                igst = taxable.multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalIgst = totalIgst.add(igst);
            } else {
                BigDecimal componentRate = gstRate.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
                cgst = taxable.multiply(componentRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                sgst = taxable.multiply(componentRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalCgst = totalCgst.add(cgst);
                totalSgst = totalSgst.add(sgst);
            }

            BigDecimal itemTotal = taxable.add(cgst).add(sgst).add(igst);
            grandTotal = grandTotal.add(itemTotal);

            InvoiceItem invItem = InvoiceItem.builder()
                    .product(item.getProduct())
                    .hsnCode(item.getProduct().getHsnCode())
                    .quantity(qty)
                    .unitPrice(rate)
                    .taxableAmount(taxable)
                    .gstRate(gstRate)
                    .cgstAmount(cgst)
                    .sgstAmount(sgst)
                    .igstAmount(igst)
                    .totalAmount(itemTotal)
                    .build();

            invoice.addItem(invItem);
        }

        invoice.setTaxableValue(totalTaxable);
        if (isInterState) {
            invoice.setIgstRate(effectiveRate(totalIgst, totalTaxable));
            invoice.setIgstAmount(totalIgst);
            invoice.setCgstRate(BigDecimal.ZERO);
            invoice.setCgstAmount(BigDecimal.ZERO);
            invoice.setSgstRate(BigDecimal.ZERO);
            invoice.setSgstAmount(BigDecimal.ZERO);
        } else {
            invoice.setCgstRate(effectiveRate(totalCgst, totalTaxable));
            invoice.setCgstAmount(totalCgst);
            invoice.setSgstRate(effectiveRate(totalSgst, totalTaxable));
            invoice.setSgstAmount(totalSgst);
            invoice.setIgstRate(BigDecimal.ZERO);
            invoice.setIgstAmount(BigDecimal.ZERO);
        }

        invoice.setGrandTotal(grandTotal);
        invoice.setOutstandingAmount(grandTotal);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Update customer outstanding balance
        BigDecimal currentCustomerBalance = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
        customer.setOutstandingBalance(currentCustomerBalance.add(grandTotal));
        customerRepository.save(customer);

        return savedInvoice;
    }

    private BigDecimal effectiveRate(BigDecimal taxAmount, BigDecimal taxableAmount) {
        if (taxableAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return taxAmount.multiply(new BigDecimal("100")).divide(taxableAmount, 2, RoundingMode.HALF_UP);
    }

    private String getFinancialYearPrefix(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month >= 4) {
            int nextYear = (year + 1) % 100;
            return String.format("INV-%d-%02d", year, nextYear);
        } else {
            int nextYear = year % 100;
            return String.format("INV-%d-%02d", year - 1, nextYear);
        }
    }

    public InvoiceResponseDto mapToDto(Invoice inv) {
        List<InvoiceItemDto> itemDtos = inv.getItems().stream().map(item -> InvoiceItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productCode(item.getProduct().getProductCode())
                .hsnCode(item.getHsnCode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .taxableAmount(item.getTaxableAmount())
                .gstRate(item.getGstRate())
                .cgstAmount(item.getCgstAmount())
                .sgstAmount(item.getSgstAmount())
                .igstAmount(item.getIgstAmount())
                .totalAmount(item.getTotalAmount())
                .build()
        ).collect(Collectors.toList());

        return InvoiceResponseDto.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .salesOrderId(inv.getSalesOrder().getId())
                .salesOrderNumber(inv.getSalesOrder().getOrderNumber())
                .customerId(inv.getCustomer().getId())
                .customerName(inv.getCustomer().getName())
                .customerCode(inv.getCustomer().getCustomerCode())
                .customerGstin(inv.getCustomerGstin())
                .customerAddress(inv.getCustomer().getAddress())
                .placeOfSupply(inv.getPlaceOfSupply())
                .isInterState(inv.isInterState())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .taxableValue(inv.getTaxableValue())
                .cgstRate(inv.getCgstRate())
                .cgstAmount(inv.getCgstAmount())
                .sgstRate(inv.getSgstRate())
                .sgstAmount(inv.getSgstAmount())
                .igstRate(inv.getIgstRate())
                .igstAmount(inv.getIgstAmount())
                .grandTotal(inv.getGrandTotal())
                .paidAmount(inv.getPaidAmount())
                .outstandingAmount(inv.getOutstandingAmount())
                .status(inv.getStatus())
                .generatedByName(inv.getGeneratedBy() != null ? inv.getGeneratedBy().getFullName() : "SYSTEM")
                .notes(inv.getNotes())
                .createdAt(inv.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}

