package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.BomDto;
import com.nittrichy.ordertoaudit.service.BomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boms")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    @GetMapping
    public ResponseEntity<List<BomDto>> getAllBoms() {
        return ResponseEntity.ok(bomService.getAllBoms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BomDto> getBomById(@PathVariable Long id) {
        return ResponseEntity.ok(bomService.getBomById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<BomDto> getBomByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(bomService.getActiveBomByProductId(productId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<BomDto> createBom(@Valid @RequestBody BomDto dto) {
        return new ResponseEntity<>(bomService.createBom(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<BomDto> updateBom(@PathVariable Long id, @Valid @RequestBody BomDto dto) {
        return ResponseEntity.ok(bomService.updateBom(id, dto));
    }
}

