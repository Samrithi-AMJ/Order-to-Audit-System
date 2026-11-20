package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.*;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.OrderStatus;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<SalesOrderResponseDto> getAllSalesOrders() {
        return salesOrderRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesOrderResponseDto getSalesOrderById(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));
        return mapToDto(order);
    }

    @Transactional(readOnly = true)
    public List<SalesOrderResponseDto> getSalesOrdersByCustomer(Long customerId) {
        return salesOrderRepository.findByCustomerId(customerId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public SalesOrderResponseDto createSalesOrder(SalesOrderRequestDto request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        User currentUser = authService.getCurrentAuthenticatedUser();

        String orderNumber = request.getOrderNumber();
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = "SO-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        SalesOrder order = SalesOrder.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .orderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now())
                .salesman(currentUser)
                .status(OrderStatus.PENDING_APPROVAL)
                .notes(request.getNotes())
                .build();

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (SalesOrderItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));

            BigDecimal qty = itemDto.getQuantity();
            BigDecimal unitPrice = itemDto.getUnitPrice() != null ? itemDto.getUnitPrice() : product.getSellingPrice();
            BigDecimal taxable = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gstRate = product.getGstRate() != null ? product.getGstRate() : new BigDecimal("5.00");
            BigDecimal gstAmount = taxable.multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = taxable.add(gstAmount);

            totalTaxable = totalTaxable.add(taxable);
            totalGst = totalGst.add(gstAmount);
            grandTotal = grandTotal.add(itemTotal);

            SalesOrderItem item = SalesOrderItem.builder()
                    .product(product)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .taxableAmount(taxable)
                    .gstRate(gstRate)
                    .gstAmount(gstAmount)
                    .totalAmount(itemTotal)
                    .build();

            order.addItem(item);
        }

        order.setTotalTaxableAmount(totalTaxable);
        order.setTotalGstAmount(totalGst);
        order.setGrandTotal(grandTotal);

        SalesOrder savedOrder = salesOrderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Transactional
    public SalesOrderResponseDto processOrderApproval(Long id, OrderApprovalDto approvalDto) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Only orders in PENDING_APPROVAL status can be approved or rejected. Current: " + order.getStatus());
        }

        User currentUser = authService.getCurrentAuthenticatedUser();
        order.setApprovedBy(currentUser);
        order.setApprovedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(approvalDto.getApproved())) {
            // Check stock availability
            for (SalesOrderItem item : order.getItems()) {
                Product product = item.getProduct();
                BigDecimal available = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
                if (item.getQuantity().compareTo(available) > 0) {
                    log.warn("Approval stock check warning: Product {} stock ({}) is less than order qty ({})",
                            product.getName(), available, item.getQuantity());
                    // Order can still be approved, but dispatch will enforce hard lock!
                }
            }
            order.setStatus(OrderStatus.APPROVED);
        } else {
            order.setStatus(OrderStatus.REJECTED);
            order.setRejectionReason(approvalDto.getRejectionReason() != null ? approvalDto.getRejectionReason() : "Rejected by Administrator");
        }

        return mapToDto(salesOrderRepository.save(order));
    }

    public SalesOrderResponseDto mapToDto(SalesOrder so) {
        List<SalesOrderItemDto> itemDtos = so.getItems().stream().map(item -> SalesOrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productCode(item.getProduct().getProductCode())
                .hsnCode(item.getProduct().getHsnCode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .taxableAmount(item.getTaxableAmount())
                .gstRate(item.getGstRate())
                .gstAmount(item.getGstAmount())
                .totalAmount(item.getTotalAmount())
                .build()
        ).collect(Collectors.toList());

        return SalesOrderResponseDto.builder()
                .id(so.getId())
                .orderNumber(so.getOrderNumber())
                .customerId(so.getCustomer().getId())
                .customerName(so.getCustomer().getName())
                .customerCode(so.getCustomer().getCustomerCode())
                .customerState(so.getCustomer().getState())
                .customerGstin(so.getCustomer().getGstin())
                .orderDate(so.getOrderDate())
                .salesmanName(so.getSalesman() != null ? so.getSalesman().getFullName() : "SYSTEM")
                .status(so.getStatus())
                .totalTaxableAmount(so.getTotalTaxableAmount())
                .totalGstAmount(so.getTotalGstAmount())
                .grandTotal(so.getGrandTotal())
                .rejectionReason(so.getRejectionReason())
                .approvedByName(so.getApprovedBy() != null ? so.getApprovedBy().getFullName() : null)
                .approvedAt(so.getApprovedAt())
                .dispatchedAt(so.getDispatchedAt())
                .notes(so.getNotes())
                .createdAt(so.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}

