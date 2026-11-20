package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.StockMovementDto;
import com.nittrichy.ordertoaudit.dto.StockSummaryDto;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/summary")
    public ResponseEntity<List<StockSummaryDto>> getAllStockSummaries() {
        return ResponseEntity.ok(inventoryService.getAllStockSummaries());
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<StockMovementDto>> getStockLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) ItemType itemType) {
        return ResponseEntity.ok(inventoryService.getStockLedger(start, end, itemType));
    }
}

