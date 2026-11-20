package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.DispatchRequestDto;
import com.nittrichy.ordertoaudit.dto.SalesOrderResponseDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.OrderStatus;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final SalesOrderRepository salesOrderRepository;
    private final InventoryService inventoryService;
    private final InvoiceService invoiceService;
    private final AuthService authService;
    private final SalesOrderService salesOrderService;

    @Transactional
    public SalesOrderResponseDto dispatchOrder(Long salesOrderId, DispatchRequestDto request) {
        SalesOrder order = salesOrderRepository.findByIdForUpdate(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", salesOrderId));

        if (order.getStatus() == OrderStatus.DISPATCHED) {
            throw new BusinessRuleException("Order " + order.getOrderNumber() + " has already been dispatched. Duplicate dispatch prevented.");
        }

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new BusinessRuleException("Only APPROVED orders can be dispatched. Current status: " + order.getStatus());
        }

        User currentUser = authService.getCurrentAuthenticatedUser();

        // 1. Rigorous pre-flight stock verification for all items
        for (SalesOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            BigDecimal available = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
            if (item.getQuantity().compareTo(available) > 0) {
                log.error("Dispatch blocked: Shortage for {} ({}). Requested: {}, Available: {}",
                        product.getName(), product.getProductCode(), item.getQuantity(), available);
                throw new InsufficientStockException(
                        product.getProductCode(),
                        product.getName(),
                        item.getQuantity(),
                        available
                );
            }
        }

        // 2. Deduct finished goods stock for each item
        for (SalesOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            inventoryService.recordProductMovement(
                    product,
                    item.getQuantity().negate(),
                    MovementType.SALES_DISPATCH,
                    item.getUnitPrice(),
                    ReferenceType.SALES_ORDER,
                    order.getId(),
                    order.getOrderNumber(),
                    "Sales dispatch for order " + order.getOrderNumber() +
                            (request != null && request.getVehicleNumber() != null ? " [Vehicle: " + request.getVehicleNumber() + "]" : ""),
                    currentUser
            );
        }

        // 3. Mark as dispatched
        order.setStatus(OrderStatus.DISPATCHED);
        order.setDispatchedAt(LocalDateTime.now());
        if (request != null && request.getDispatchNotes() != null) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + "; " : "") + "Dispatch Notes: " + request.getDispatchNotes());
        }
        SalesOrder savedOrder = salesOrderRepository.save(order);

        // 4. Generate invoice automatically if not yet generated
        invoiceService.generateInvoiceForOrder(savedOrder.getId());

        return salesOrderService.mapToDto(savedOrder);
    }
}

