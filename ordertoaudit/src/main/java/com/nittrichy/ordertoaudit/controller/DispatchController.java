package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.DispatchRequestDto;
import com.nittrichy.ordertoaudit.dto.SalesOrderResponseDto;
import com.nittrichy.ordertoaudit.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SalesOrderResponseDto> dispatchOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) DispatchRequestDto request) {
        return ResponseEntity.ok(dispatchService.dispatchOrder(orderId, request));
    }
}

