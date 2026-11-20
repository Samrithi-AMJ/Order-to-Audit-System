package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.PurchaseRequestDto;
import com.nittrichy.ordertoaudit.dto.PurchaseResponseDto;
import com.nittrichy.ordertoaudit.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ResponseEntity<List<PurchaseResponseDto>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponseDto> getPurchaseById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<PurchaseResponseDto> recordPurchase(@Valid @RequestBody PurchaseRequestDto request) {
        return new ResponseEntity<>(purchaseService.recordPurchase(request), HttpStatus.CREATED);
    }
}

