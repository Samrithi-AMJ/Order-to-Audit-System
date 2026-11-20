package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.OrderApprovalDto;
import com.nittrichy.ordertoaudit.dto.SalesOrderRequestDto;
import com.nittrichy.ordertoaudit.dto.SalesOrderResponseDto;
import com.nittrichy.ordertoaudit.service.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    public ResponseEntity<List<SalesOrderResponseDto>> getAllSalesOrders() {
        return ResponseEntity.ok(salesOrderService.getAllSalesOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponseDto> getSalesOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.getSalesOrderById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SalesOrderResponseDto>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(salesOrderService.getSalesOrdersByCustomer(customerId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SALESMAN', 'ADMIN', 'OWNER')")
    public ResponseEntity<SalesOrderResponseDto> createSalesOrder(@Valid @RequestBody SalesOrderRequestDto request) {
        return new ResponseEntity<>(salesOrderService.createSalesOrder(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SalesOrderResponseDto> processOrderApproval(
            @PathVariable Long id,
            @Valid @RequestBody OrderApprovalDto approvalDto) {
        return ResponseEntity.ok(salesOrderService.processOrderApproval(id, approvalDto));
    }
}

