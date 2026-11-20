package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.*;
import com.nittrichy.ordertoaudit.service.MasterDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    // --- PRODUCTS ---
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(masterDataService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(masterDataService.getProductById(id));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto dto) {
        return new ResponseEntity<>(masterDataService.createProduct(dto), HttpStatus.CREATED);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto dto) {
        return ResponseEntity.ok(masterDataService.updateProduct(id, dto));
    }

    // --- CUSTOMERS ---
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(masterDataService.getAllCustomers());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(masterDataService.getCustomerById(id));
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESMAN', 'OWNER')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto dto) {
        return new ResponseEntity<>(masterDataService.createCustomer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(masterDataService.updateCustomer(id, dto));
    }

    // --- SUPPLIERS ---
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDto>> getAllSuppliers() {
        return ResponseEntity.ok(masterDataService.getAllSuppliers());
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(masterDataService.getSupplierById(id));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<SupplierDto> createSupplier(@Valid @RequestBody SupplierDto dto) {
        return new ResponseEntity<>(masterDataService.createSupplier(dto), HttpStatus.CREATED);
    }

    // --- RAW MATERIALS ---
    @GetMapping("/raw-materials")
    public ResponseEntity<List<RawMaterialDto>> getAllRawMaterials() {
        return ResponseEntity.ok(masterDataService.getAllRawMaterials());
    }

    @GetMapping("/raw-materials/{id}")
    public ResponseEntity<RawMaterialDto> getRawMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(masterDataService.getRawMaterialById(id));
    }

    @PostMapping("/raw-materials")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<RawMaterialDto> createRawMaterial(@Valid @RequestBody RawMaterialDto dto) {
        return new ResponseEntity<>(masterDataService.createRawMaterial(dto), HttpStatus.CREATED);
    }

    // --- PACKAGING MATERIALS ---
    @GetMapping("/packaging-materials")
    public ResponseEntity<List<PackagingMaterialDto>> getAllPackagingMaterials() {
        return ResponseEntity.ok(masterDataService.getAllPackagingMaterials());
    }

    @GetMapping("/packaging-materials/{id}")
    public ResponseEntity<PackagingMaterialDto> getPackagingMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(masterDataService.getPackagingMaterialById(id));
    }

    @PostMapping("/packaging-materials")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PRODUCTION')")
    public ResponseEntity<PackagingMaterialDto> createPackagingMaterial(@Valid @RequestBody PackagingMaterialDto dto) {
        return new ResponseEntity<>(masterDataService.createPackagingMaterial(dto), HttpStatus.CREATED);
    }
}

