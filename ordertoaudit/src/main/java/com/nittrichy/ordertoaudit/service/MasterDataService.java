package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.*;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final AuthService authService;

    // ================= PRODUCT =================
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapProductToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapProductToDto(product);
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        if (productRepository.existsByProductCode(dto.getProductCode())) {
            throw new BusinessRuleException("Product code '" + dto.getProductCode() + "' already exists");
        }

        BigDecimal openingStock = dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO;
        validateOpeningStock(openingStock);
        Product product = Product.builder()
                .productCode(dto.getProductCode())
                .name(dto.getName())
                .category(dto.getCategory())
                .unit(dto.getUnit())
                .packetWeightGrams(dto.getPacketWeightGrams())
                .hsnCode(dto.getHsnCode())
                .sellingPrice(dto.getSellingPrice())
                .gstRate(dto.getGstRate() != null ? dto.getGstRate() : new BigDecimal("5.00"))
                .currentStock(BigDecimal.ZERO)
                .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : BigDecimal.ZERO)
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        if (openingStock.compareTo(BigDecimal.ZERO) > 0) {
            inventoryService.recordProductMovement(savedProduct, openingStock, MovementType.OPENING_BALANCE,
                    savedProduct.getSellingPrice(), ReferenceType.INITIAL_SETUP, null,
                    "OPENING-" + savedProduct.getProductCode(), "Opening stock on product creation", authService.getCurrentAuthenticatedUser());
        }
        return mapProductToDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setUnit(dto.getUnit());
        product.setPacketWeightGrams(dto.getPacketWeightGrams());
        product.setHsnCode(dto.getHsnCode());
        product.setSellingPrice(dto.getSellingPrice());
        if (dto.getGstRate() != null) product.setGstRate(dto.getGstRate());
        if (dto.getReorderLevel() != null) product.setReorderLevel(dto.getReorderLevel());
        product.setActive(dto.isActive());

        return mapProductToDto(productRepository.save(product));
    }

    // ================= CUSTOMER =================
    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::mapCustomerToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return mapCustomerToDto(customer);
    }

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto) {
        if (customerRepository.existsByCustomerCode(dto.getCustomerCode())) {
            throw new BusinessRuleException("Customer code '" + dto.getCustomerCode() + "' already exists");
        }

        User salesman = null;
        if (dto.getAssignedSalesmanId() != null) {
            salesman = userRepository.findById(dto.getAssignedSalesmanId()).orElse(null);
        }

        Customer customer = Customer.builder()
                .customerCode(dto.getCustomerCode())
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .state(dto.getState())
                .stateCode(dto.getStateCode())
                .gstin(dto.getGstin())
                .creditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .assignedSalesman(salesman)
                .active(true)
                .build();

        return mapCustomerToDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDto updateCustomer(Long id, CustomerDto dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setName(dto.getName());
        customer.setContactPerson(dto.getContactPerson());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setState(dto.getState());
        customer.setStateCode(dto.getStateCode());
        customer.setGstin(dto.getGstin());
        if (dto.getCreditLimit() != null) customer.setCreditLimit(dto.getCreditLimit());
        if (dto.getAssignedSalesmanId() != null) {
            customer.setAssignedSalesman(userRepository.findById(dto.getAssignedSalesmanId()).orElse(null));
        }
        customer.setActive(dto.isActive());

        return mapCustomerToDto(customerRepository.save(customer));
    }

    // ================= SUPPLIER =================
    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::mapSupplierToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return mapSupplierToDto(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(SupplierDto dto) {
        if (supplierRepository.existsBySupplierCode(dto.getSupplierCode())) {
            throw new BusinessRuleException("Supplier code '" + dto.getSupplierCode() + "' already exists");
        }

        Supplier supplier = Supplier.builder()
                .supplierCode(dto.getSupplierCode())
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .state(dto.getState())
                .gstin(dto.getGstin())
                .active(true)
                .build();

        return mapSupplierToDto(supplierRepository.save(supplier));
    }

    // ================= RAW MATERIAL =================
    @Transactional(readOnly = true)
    public List<RawMaterialDto> getAllRawMaterials() {
        return rawMaterialRepository.findAll().stream().map(this::mapRawMaterialToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RawMaterialDto getRawMaterialById(Long id) {
        RawMaterial rm = rawMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RawMaterial", "id", id));
        return mapRawMaterialToDto(rm);
    }

    @Transactional
    public RawMaterialDto createRawMaterial(RawMaterialDto dto) {
        if (rawMaterialRepository.existsByMaterialCode(dto.getMaterialCode())) {
            throw new BusinessRuleException("Raw material code '" + dto.getMaterialCode() + "' already exists");
        }

        BigDecimal openingStock = dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO;
        validateOpeningStock(openingStock);
        RawMaterial rm = RawMaterial.builder()
                .materialCode(dto.getMaterialCode())
                .name(dto.getName())
                .category(dto.getCategory())
                .unit(dto.getUnit())
                .currentStock(BigDecimal.ZERO)
                .weightedAvgCost(dto.getWeightedAvgCost() != null ? dto.getWeightedAvgCost() : BigDecimal.ZERO)
                .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : BigDecimal.ZERO)
                .active(true)
                .build();

        RawMaterial savedMaterial = rawMaterialRepository.save(rm);
        if (openingStock.compareTo(BigDecimal.ZERO) > 0) {
            inventoryService.recordRawMaterialMovement(savedMaterial, openingStock, MovementType.OPENING_BALANCE,
                    savedMaterial.getWeightedAvgCost(), ReferenceType.INITIAL_SETUP, null,
                    "OPENING-" + savedMaterial.getMaterialCode(), "Opening stock on raw material creation", authService.getCurrentAuthenticatedUser());
        }
        return mapRawMaterialToDto(savedMaterial);
    }

    // ================= PACKAGING MATERIAL =================
    @Transactional(readOnly = true)
    public List<PackagingMaterialDto> getAllPackagingMaterials() {
        return packagingMaterialRepository.findAll().stream().map(this::mapPackagingMaterialToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PackagingMaterialDto getPackagingMaterialById(Long id) {
        PackagingMaterial pm = packagingMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PackagingMaterial", "id", id));
        return mapPackagingMaterialToDto(pm);
    }

    @Transactional
    public PackagingMaterialDto createPackagingMaterial(PackagingMaterialDto dto) {
        if (packagingMaterialRepository.existsByMaterialCode(dto.getMaterialCode())) {
            throw new BusinessRuleException("Packaging material code '" + dto.getMaterialCode() + "' already exists");
        }

        BigDecimal openingStock = dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO;
        validateOpeningStock(openingStock);
        PackagingMaterial pm = PackagingMaterial.builder()
                .materialCode(dto.getMaterialCode())
                .name(dto.getName())
                .type(dto.getType())
                .unit(dto.getUnit())
                .currentStock(BigDecimal.ZERO)
                .unitCost(dto.getUnitCost() != null ? dto.getUnitCost() : BigDecimal.ZERO)
                .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : BigDecimal.ZERO)
                .active(true)
                .build();

        PackagingMaterial savedMaterial = packagingMaterialRepository.save(pm);
        if (openingStock.compareTo(BigDecimal.ZERO) > 0) {
            inventoryService.recordPackagingMaterialMovement(savedMaterial, openingStock, MovementType.OPENING_BALANCE,
                    savedMaterial.getUnitCost(), ReferenceType.INITIAL_SETUP, null,
                    "OPENING-" + savedMaterial.getMaterialCode(), "Opening stock on packaging material creation", authService.getCurrentAuthenticatedUser());
        }
        return mapPackagingMaterialToDto(savedMaterial);
    }

    private void validateOpeningStock(BigDecimal openingStock) {
        if (openingStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Opening stock cannot be negative");
        }
    }

    // ================= MAPPERS =================
    public ProductDto mapProductToDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .productCode(p.getProductCode())
                .name(p.getName())
                .category(p.getCategory())
                .unit(p.getUnit())
                .packetWeightGrams(p.getPacketWeightGrams())
                .hsnCode(p.getHsnCode())
                .sellingPrice(p.getSellingPrice())
                .gstRate(p.getGstRate())
                .currentStock(p.getCurrentStock())
                .reorderLevel(p.getReorderLevel())
                .active(p.isActive())
                .build();
    }

    public CustomerDto mapCustomerToDto(Customer c) {
        return CustomerDto.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .name(c.getName())
                .contactPerson(c.getContactPerson())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .state(c.getState())
                .stateCode(c.getStateCode())
                .gstin(c.getGstin())
                .creditLimit(c.getCreditLimit())
                .outstandingBalance(c.getOutstandingBalance())
                .assignedSalesmanId(c.getAssignedSalesman() != null ? c.getAssignedSalesman().getId() : null)
                .assignedSalesmanName(c.getAssignedSalesman() != null ? c.getAssignedSalesman().getFullName() : null)
                .active(c.isActive())
                .build();
    }

    public SupplierDto mapSupplierToDto(Supplier s) {
        return SupplierDto.builder()
                .id(s.getId())
                .supplierCode(s.getSupplierCode())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .state(s.getState())
                .gstin(s.getGstin())
                .active(s.isActive())
                .build();
    }

    public RawMaterialDto mapRawMaterialToDto(RawMaterial rm) {
        return RawMaterialDto.builder()
                .id(rm.getId())
                .materialCode(rm.getMaterialCode())
                .name(rm.getName())
                .category(rm.getCategory())
                .unit(rm.getUnit())
                .currentStock(rm.getCurrentStock())
                .weightedAvgCost(rm.getWeightedAvgCost())
                .reorderLevel(rm.getReorderLevel())
                .active(rm.isActive())
                .build();
    }

    public PackagingMaterialDto mapPackagingMaterialToDto(PackagingMaterial pm) {
        return PackagingMaterialDto.builder()
                .id(pm.getId())
                .materialCode(pm.getMaterialCode())
                .name(pm.getName())
                .type(pm.getType())
                .unit(pm.getUnit())
                .currentStock(pm.getCurrentStock())
                .unitCost(pm.getUnitCost())
                .reorderLevel(pm.getReorderLevel())
                .active(pm.isActive())
                .build();
    }
}

