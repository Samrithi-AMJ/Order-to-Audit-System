package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.ProductionRunRequestDto;
import com.nittrichy.ordertoaudit.dto.ProductionRunResponseDto;
import com.nittrichy.ordertoaudit.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    @GetMapping
    public ResponseEntity<List<ProductionRunResponseDto>> getAllProductionRuns() {
        return ResponseEntity.ok(productionService.getAllProductionRuns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductionRunResponseDto> getProductionRunById(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.getProductionRunById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<ProductionRunResponseDto> recordProductionRun(@Valid @RequestBody ProductionRunRequestDto request) {
        return new ResponseEntity<>(productionService.recordProductionRun(request), HttpStatus.CREATED);
    }
}

