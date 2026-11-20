package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.*;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.*;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchmarkService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;
    private final BOMRepository bomRepository;
    private final BOMItemRepository bomItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductionRunRepository productionRunRepository;
    private final MaterialConsumptionRepository materialConsumptionRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PaymentRepository paymentRepository;
    private final Environment environment;

    @Value("${app.benchmark.enabled:false}")
    private boolean benchmarkEnabled;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    private final InventoryService inventoryService;
    private final PurchaseService purchaseService;
    private final ProductionService productionService;
    private final SalesOrderService salesOrderService;
    private final DispatchService dispatchService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    @Transactional
    public BenchmarkResponseDto runBenchmark() {
        assertSafeBenchmarkEnvironment();
        assertBusinessTablesEmpty();
        log.info("Starting execution of Mandatory Benchmark Scenario...");

        // Benchmark data is additive and runs only in a dedicated empty H2 demo/test database.
        User adminUser = null;

        // A. Raw Materials
        RawMaterial flour = rawMaterialRepository.save(RawMaterial.builder()
                .materialCode("RM-FLOUR")
                .name("Flour")
                .category(MaterialCategory.FLOUR)
                .unit("KG")
                .currentStock(BigDecimal.ZERO)
                .weightedAvgCost(new BigDecimal("42.00")) // Assignment opening rate
                .reorderLevel(new BigDecimal("20.00"))
                .active(true)
                .build());

        RawMaterial oil = rawMaterialRepository.save(RawMaterial.builder()
                .materialCode("RM-OIL")
                .name("Cooking oil")
                .category(MaterialCategory.OIL)
                .unit("LITRE")
                .currentStock(BigDecimal.ZERO)
                .weightedAvgCost(new BigDecimal("140.00"))
                .reorderLevel(new BigDecimal("10.00"))
                .active(true)
                .build());

        RawMaterial spice = rawMaterialRepository.save(RawMaterial.builder()
                .materialCode("RM-SPICE")
                .name("Spice mix")
                .category(MaterialCategory.SPICE)
                .unit("KG")
                .currentStock(BigDecimal.ZERO)
                .weightedAvgCost(new BigDecimal("200.00"))
                .reorderLevel(new BigDecimal("2.00"))
                .active(true)
                .build());

        // Initial Raw Material Stocks
        inventoryService.recordRawMaterialMovement(flour, new BigDecimal("100.000"), MovementType.OPENING_BALANCE,
                new BigDecimal("42.00"), ReferenceType.INITIAL_SETUP, null, "INIT-RM-FLOUR", "Initial stock Flour 100 kg", adminUser);

        inventoryService.recordRawMaterialMovement(oil, new BigDecimal("30.000"), MovementType.OPENING_BALANCE,
                new BigDecimal("140.00"), ReferenceType.INITIAL_SETUP, null, "INIT-RM-OIL", "Initial stock Cooking oil 30 L", adminUser);

        inventoryService.recordRawMaterialMovement(spice, new BigDecimal("5.000"), MovementType.OPENING_BALANCE,
                new BigDecimal("200.00"), ReferenceType.INITIAL_SETUP, null, "INIT-RM-SPICE", "Initial stock Spice mix 5 kg", adminUser);

        // B. Packaging Materials
        PackagingMaterial pouches = packagingMaterialRepository.save(PackagingMaterial.builder()
                .materialCode("PM-POUCH")
                .name("Pouches")
                .type(PackagingType.POUCH)
                .unit("PIECE")
                .currentStock(BigDecimal.ZERO)
                .unitCost(new BigDecimal("2.50"))
                .reorderLevel(new BigDecimal("100.00"))
                .active(true)
                .build());

        PackagingMaterial cartons = packagingMaterialRepository.save(PackagingMaterial.builder()
                .materialCode("PM-CARTON")
                .name("Cartons")
                .type(PackagingType.CARTON)
                .unit("PIECE")
                .currentStock(BigDecimal.ZERO)
                .unitCost(new BigDecimal("15.00"))
                .reorderLevel(new BigDecimal("10.00"))
                .active(true)
                .build());

        inventoryService.recordPackagingMaterialMovement(pouches, new BigDecimal("500.000"), MovementType.OPENING_BALANCE,
                new BigDecimal("2.50"), ReferenceType.INITIAL_SETUP, null, "INIT-PM-POUCH", "Initial stock Pouches 500 pcs", adminUser);

        inventoryService.recordPackagingMaterialMovement(cartons, new BigDecimal("30.000"), MovementType.OPENING_BALANCE,
                new BigDecimal("15.00"), ReferenceType.INITIAL_SETUP, null, "INIT-PM-CARTON", "Initial stock Cartons 30 pcs", adminUser);

        // C. Product: Finished Murukku 200g
        Product murukku = productRepository.save(Product.builder()
                .productCode("PROD-MRK-200")
                .name("Murukku 200g")
                .category(ProductCategory.MURUKKU)
                .unit("PACKET")
                .packetWeightGrams(200)
                .hsnCode("1905")
                .sellingPrice(new BigDecimal("50.00"))
                .gstRate(new BigDecimal("5.00"))
                .currentStock(BigDecimal.ZERO)
                .reorderLevel(new BigDecimal("20.00"))
                .active(true)
                .build());

        inventoryService.recordProductMovement(murukku, new BigDecimal("50.000"), MovementType.OPENING_BALANCE,
                new BigDecimal("50.00"), ReferenceType.INITIAL_SETUP, null, "INIT-PROD-MRK", "Initial stock Murukku 50 pkts", adminUser);

        // D. BOM for Murukku 200g (Batch size: 200 packets)
        BOM bom = BOM.builder()
                .bomCode("BOM-MRK-200")
                .product(murukku)
                .batchSize(new BigDecimal("200.000"))
                .description("Standard BOM for 200 packets of 200g Murukku")
                .version(1)
                .active(true)
                .build();

        bom.addItem(BOMItem.builder().itemType(ItemType.RAW_MATERIAL).rawMaterial(flour).standardQuantity(new BigDecimal("40.000")).unit("KG").build());
        bom.addItem(BOMItem.builder().itemType(ItemType.RAW_MATERIAL).rawMaterial(oil).standardQuantity(new BigDecimal("10.000")).unit("LITRE").build());
        bom.addItem(BOMItem.builder().itemType(ItemType.RAW_MATERIAL).rawMaterial(spice).standardQuantity(new BigDecimal("1.200")).unit("KG").build());
        bom.addItem(BOMItem.builder().itemType(ItemType.PACKAGING_MATERIAL).packagingMaterial(pouches).standardQuantity(new BigDecimal("200.000")).unit("PIECE").build());
        bom.addItem(BOMItem.builder().itemType(ItemType.PACKAGING_MATERIAL).packagingMaterial(cartons).standardQuantity(new BigDecimal("10.000")).unit("PIECE").build());
        bom = bomRepository.save(bom);

        // E. Customers & Supplier
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-AGRO-01")
                .name("Trichy Agro Suppliers")
                .contactPerson("Venkatesh")
                .phone("9876543210")
                .address("10 Market Street, Tiruchirappalli")
                .state("Tamil Nadu")
                .gstin("33AABCS9876C1Z1")
                .active(true)
                .build());

        Customer retailerA = customerRepository.save(Customer.builder()
                .customerCode("CUST-RET-A")
                .name("Retailer A")
                .contactPerson("Murugan")
                .phone("9841012345")
                .address("45 Bazaar Street, Madurai")
                .state("Tamil Nadu")
                .stateCode("33")
                .gstin("33AABCT1234A1Z5")
                .creditLimit(new BigDecimal("50000.00"))
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build());

        Customer distributorB = customerRepository.save(Customer.builder()
                .customerCode("CUST-DIST-B")
                .name("Distributor B")
                .contactPerson("Anand")
                .phone("9448012345")
                .address("77 Brigade Road, Bengaluru")
                .state("Karnataka")
                .stateCode("29")
                .gstin("29AABCD5678B1Z2")
                .creditLimit(new BigDecimal("100000.00"))
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build());

        // ==========================================
        // 4. BENCHMARK STEP 1: PURCHASE
        // Purchase 60 kg flour at ₹45/kg
        // ==========================================
        PurchaseRequestDto purchaseReq = PurchaseRequestDto.builder()
                .purchaseNumber("PO-BENCH-001")
                .supplierId(supplier.getId())
                .purchaseDate(LocalDate.now())
                .supplierInvoiceNo("INV-SUP-901")
                .notes("Benchmark Purchase: 60 kg flour @ ₹45/kg")
                .items(List.of(
                        PurchaseItemDto.builder()
                                .itemType(ItemType.RAW_MATERIAL)
                                .rawMaterialId(flour.getId())
                                .quantity(new BigDecimal("60.000"))
                                .unitPrice(new BigDecimal("45.00"))
                                .build()
                ))
                .build();
        purchaseService.recordPurchase(purchaseReq);

        // ==========================================
        // 5. BENCHMARK STEP 2: PRODUCTION RUN
        // Output: 188 good packets
        // Consumptions: 40 kg flour, 10.5 L oil, 1.2 kg spice
        // Packaging: 188 good pouches + 6 damaged = 194; 9 cartons
        // Labour: ₹1,200; Overhead: ₹600
        // ==========================================
        ProductionRunRequestDto prodReq = ProductionRunRequestDto.builder()
                .runNumber("PR-BENCH-001")
                .productId(murukku.getId())
                .bomId(bom.getId())
                .batchMultiplier(BigDecimal.ONE)
                .actualGoodOutput(new BigDecimal("188.000"))
                .labourCost(new BigDecimal("1200.00"))
                .overheadCost(new BigDecimal("600.00"))
                .runDate(LocalDate.now())
                .notes("Benchmark Production Run")
                .consumptions(List.of(
                        MaterialConsumptionDto.builder()
                                .itemType(ItemType.RAW_MATERIAL)
                                .rawMaterialId(flour.getId())
                                .actualQuantity(new BigDecimal("40.000"))
                                .damagedQuantity(BigDecimal.ZERO)
                                .build(),
                        MaterialConsumptionDto.builder()
                                .itemType(ItemType.RAW_MATERIAL)
                                .rawMaterialId(oil.getId())
                                .actualQuantity(new BigDecimal("10.500"))
                                .damagedQuantity(BigDecimal.ZERO)
                                .build(),
                        MaterialConsumptionDto.builder()
                                .itemType(ItemType.RAW_MATERIAL)
                                .rawMaterialId(spice.getId())
                                .actualQuantity(new BigDecimal("1.200"))
                                .damagedQuantity(BigDecimal.ZERO)
                                .build(),
                        MaterialConsumptionDto.builder()
                                .itemType(ItemType.PACKAGING_MATERIAL)
                                .packagingMaterialId(pouches.getId())
                                .actualQuantity(new BigDecimal("188.000"))
                                .damagedQuantity(new BigDecimal("6.000"))
                                .build(),
                        MaterialConsumptionDto.builder()
                                .itemType(ItemType.PACKAGING_MATERIAL)
                                .packagingMaterialId(cartons.getId())
                                .actualQuantity(new BigDecimal("9.000"))
                                .damagedQuantity(BigDecimal.ZERO)
                                .build()
                ))
                .build();
        ProductionRunResponseDto prodRunResp = productionService.recordProductionRun(prodReq);

        BigDecimal finishedGoodsPostProd = productRepository.findById(murukku.getId()).get().getCurrentStock();

        // ==========================================
        // 6. BENCHMARK STEP 3: RETAILER A SALES ORDER & DISPATCH
        // Retailer A (Tamil Nadu): 150 packets @ ₹50.00
        // GST 5% (CGST ₹187.50, SGST ₹187.50) => Total ₹7,875.00
        // Payment: ₹4,000 => Outstanding: ₹3,875.00
        // ==========================================
        SalesOrderRequestDto orderReqA = SalesOrderRequestDto.builder()
                .orderNumber("SO-BENCH-001")
                .customerId(retailerA.getId())
                .orderDate(LocalDate.now())
                .notes("Retailer A Benchmark Order")
                .items(List.of(
                        SalesOrderItemDto.builder()
                                .productId(murukku.getId())
                                .quantity(new BigDecimal("150.000"))
                                .unitPrice(new BigDecimal("50.00"))
                                .build()
                ))
                .build();
        SalesOrderResponseDto orderA = salesOrderService.createSalesOrder(orderReqA);

        // Approve Order A
        salesOrderService.processOrderApproval(orderA.getId(), OrderApprovalDto.builder().approved(true).build());

        // Dispatch Order A
        dispatchService.dispatchOrder(orderA.getId(), DispatchRequestDto.builder().dispatchNotes("Benchmark dispatch Retailer A").build());

        // Fetch generated invoice
        Invoice invoiceA = invoiceRepository.findBySalesOrderId(orderA.getId()).orElseThrow();

        // Record payment of ₹4,000
        PaymentRequestDto paymentReq = PaymentRequestDto.builder()
                .customerId(retailerA.getId())
                .invoiceId(invoiceA.getId())
                .amount(new BigDecimal("4000.00"))
                .paymentDate(LocalDate.now())
                .paymentMode(PaymentMode.NEFT)
                .referenceNumber("NEFT-TN-4000")
                .notes("Benchmark partial payment")
                .build();
        paymentService.recordPayment(paymentReq);

        // Refresh invoice after payment
        invoiceA = invoiceRepository.findById(invoiceA.getId()).orElseThrow();
        BigDecimal finishedGoodsPostSale = productRepository.findById(murukku.getId()).get().getCurrentStock();

        // ==========================================
        // 7. BENCHMARK STEP 4: DISTRIBUTOR B VALIDATION
        // Distributor B (Karnataka): Order 120 packets
        // Available finished stock: 88 packets
        // System must flag shortage of 32 packets & block dispatch!
        // ==========================================
        SalesOrderRequestDto orderReqB = SalesOrderRequestDto.builder()
                .orderNumber("SO-BENCH-002")
                .customerId(distributorB.getId())
                .orderDate(LocalDate.now())
                .notes("Distributor B Benchmark Order (Shortage test)")
                .items(List.of(
                        SalesOrderItemDto.builder()
                                .productId(murukku.getId())
                                .quantity(new BigDecimal("120.000"))
                                .unitPrice(new BigDecimal("50.00"))
                                .build()
                ))
                .build();
        SalesOrderResponseDto orderB = salesOrderService.createSalesOrder(orderReqB);
        salesOrderService.processOrderApproval(orderB.getId(), OrderApprovalDto.builder().approved(true).build());

        boolean dispatchBlocked = false;
        String blockReason = null;
        BigDecimal shortageQty = BigDecimal.ZERO;

        try {
            dispatchService.dispatchOrder(orderB.getId(), DispatchRequestDto.builder().dispatchNotes("Attempted dispatch Distributor B").build());
        } catch (InsufficientStockException ex) {
            dispatchBlocked = true;
            blockReason = ex.getMessage();
            shortageQty = ex.getShortageQuantity();
            log.info("Distributor B dispatch successfully blocked: {}", ex.getMessage());
        }

        // ==========================================
        // 8. VERIFY ACTUAL CLOSING STOCKS & FORMULATE CHECKS
        // ==========================================
        RawMaterial flourActual = rawMaterialRepository.findById(flour.getId()).get();
        RawMaterial oilActual = rawMaterialRepository.findById(oil.getId()).get();
        RawMaterial spiceActual = rawMaterialRepository.findById(spice.getId()).get();
        PackagingMaterial pouchActual = packagingMaterialRepository.findById(pouches.getId()).get();
        PackagingMaterial cartonActual = packagingMaterialRepository.findById(cartons.getId()).get();

        // Variance
        BigDecimal oilVariance = prodRunResp.getConsumptions().stream()
                .filter(c -> c.getItemType() == ItemType.RAW_MATERIAL && "Cooking oil".equalsIgnoreCase(c.getRawMaterialName()))
                .findFirst()
                .map(MaterialConsumptionDto::getVariancePercentage)
                .orElse(BigDecimal.ZERO);

        List<BenchmarkResponseDto.BenchmarkCheck> checks = new ArrayList<>();

        addCheck(checks, "Flour Closing Stock", "120.000 kg", flourActual.getCurrentStock().stripTrailingZeros().toPlainString() + " kg",
                flourActual.getCurrentStock().compareTo(new BigDecimal("120.000")) == 0);

        addCheck(checks, "Cooking Oil Closing Stock", "19.500 litres", oilActual.getCurrentStock().stripTrailingZeros().toPlainString() + " litres",
                oilActual.getCurrentStock().compareTo(new BigDecimal("19.500")) == 0);

        addCheck(checks, "Spice Mix Closing Stock", "3.800 kg", spiceActual.getCurrentStock().stripTrailingZeros().toPlainString() + " kg",
                spiceActual.getCurrentStock().compareTo(new BigDecimal("3.800")) == 0);

        addCheck(checks, "Pouches Closing Stock", "306.000 pieces", pouchActual.getCurrentStock().stripTrailingZeros().toPlainString() + " pieces",
                pouchActual.getCurrentStock().compareTo(new BigDecimal("306.000")) == 0);

        addCheck(checks, "Cartons Closing Stock", "21.000 pieces", cartonActual.getCurrentStock().stripTrailingZeros().toPlainString() + " pieces",
                cartonActual.getCurrentStock().compareTo(new BigDecimal("21.000")) == 0);

        addCheck(checks, "Finished Murukku Post-Production", "238.000 packets", finishedGoodsPostProd.stripTrailingZeros().toPlainString() + " packets",
                finishedGoodsPostProd.compareTo(new BigDecimal("238.000")) == 0);

        addCheck(checks, "Finished Murukku Post-Retailer A Dispatch", "88.000 packets", finishedGoodsPostSale.stripTrailingZeros().toPlainString() + " packets",
                finishedGoodsPostSale.compareTo(new BigDecimal("88.000")) == 0);

        addCheck(checks, "Production Yield Variance", "-6.00%", prodRunResp.getYieldVariancePercentage().toPlainString() + "%",
                prodRunResp.getYieldVariancePercentage().compareTo(new BigDecimal("-6.00")) == 0);

        addCheck(checks, "Oil Consumption Variance", "+5.00%", oilVariance.toPlainString() + "%",
                oilVariance.compareTo(new BigDecimal("5.00")) == 0);

        addCheck(checks, "Retailer A Taxable Value", "₹7,500.00", "₹" + invoiceA.getTaxableValue().toPlainString(),
                invoiceA.getTaxableValue().compareTo(new BigDecimal("7500.00")) == 0);

        addCheck(checks, "Retailer A CGST & SGST (Intra-state 2.5% each)", "CGST ₹187.50, SGST ₹187.50",
                "CGST ₹" + invoiceA.getCgstAmount().toPlainString() + ", SGST ₹" + invoiceA.getSgstAmount().toPlainString(),
                invoiceA.getCgstAmount().compareTo(new BigDecimal("187.50")) == 0 && invoiceA.getSgstAmount().compareTo(new BigDecimal("187.50")) == 0);

        addCheck(checks, "Retailer A Invoice Grand Total", "₹7,875.00", "₹" + invoiceA.getGrandTotal().toPlainString(),
                invoiceA.getGrandTotal().compareTo(new BigDecimal("7875.00")) == 0);

        addCheck(checks, "Retailer A Outstanding Balance", "₹3,875.00", "₹" + invoiceA.getOutstandingAmount().toPlainString(),
                invoiceA.getOutstandingAmount().compareTo(new BigDecimal("3875.00")) == 0);

        addCheck(checks, "Distributor B Shortage Detection", "Shortage: 32.000 packets", "Shortage: " + shortageQty.stripTrailingZeros().toPlainString() + " packets",
                shortageQty.compareTo(new BigDecimal("32.000")) == 0);

        addCheck(checks, "Distributor B Invalid Dispatch Blocked", "BLOCKED (True)", "BLOCKED (" + dispatchBlocked + ")",
                dispatchBlocked);

        boolean allPassed = checks.stream().allMatch(BenchmarkResponseDto.BenchmarkCheck::isPassed);

        // Costing metrics
        BigDecimal sellingPrice = murukku.getSellingPrice();
        BigDecimal costPerGood = prodRunResp.getCostPerPacket();
        BigDecimal marginPerPacket = sellingPrice.subtract(costPerGood);
        BigDecimal marginPct = sellingPrice.compareTo(BigDecimal.ZERO) > 0
                ? marginPerPacket.divide(sellingPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BenchmarkResponseDto response = BenchmarkResponseDto.builder()
                .success(allPassed)
                .message(allPassed ? "Mandatory Benchmark Scenario completed successfully with 100% accuracy!"
                        : "Benchmark completed with variance in some verification checks.")
                .stockBalances(BenchmarkResponseDto.BenchmarkStockResult.builder()
                        .flourClosingStock(flourActual.getCurrentStock())
                        .oilClosingStock(oilActual.getCurrentStock())
                        .spiceClosingStock(spiceActual.getCurrentStock())
                        .pouchClosingStock(pouchActual.getCurrentStock())
                        .cartonClosingStock(cartonActual.getCurrentStock())
                        .finishedGoodsPostProd(finishedGoodsPostProd)
                        .finishedGoodsPostSale(finishedGoodsPostSale)
                        .build())
                .varianceResults(BenchmarkResponseDto.BenchmarkVarianceResult.builder()
                        .yieldVariancePercentage(prodRunResp.getYieldVariancePercentage())
                        .oilConsumptionVariance(oilVariance)
                        .yieldVarianceFlagged(prodRunResp.isVarianceFlag())
                        .oilVarianceFlagged(oilVariance.abs().compareTo(new BigDecimal("3.00")) > 0)
                        .build())
                .retailerASalesResult(BenchmarkResponseDto.BenchmarkSalesResult.builder()
                        .customerName(retailerA.getName())
                        .quantity(new BigDecimal("150.000"))
                        .unitPrice(murukku.getSellingPrice())
                        .taxableValue(invoiceA.getTaxableValue())
                        .cgstAmount(invoiceA.getCgstAmount())
                        .sgstAmount(invoiceA.getSgstAmount())
                        .igstAmount(invoiceA.getIgstAmount())
                        .invoiceTotal(invoiceA.getGrandTotal())
                        .paymentReceived(invoiceA.getPaidAmount())
                        .outstandingAmount(invoiceA.getOutstandingAmount())
                        .build())
                .distributorBResult(BenchmarkResponseDto.BenchmarkDistributorBResult.builder()
                        .customerName(distributorB.getName())
                        .orderedQuantity(new BigDecimal("120.000"))
                        .availableQuantity(finishedGoodsPostSale)
                        .shortageQuantity(shortageQty)
                        .dispatchBlocked(dispatchBlocked)
                        .blockReason(blockReason)
                        .build())
                .costingResult(BenchmarkResponseDto.BenchmarkCostingResult.builder()
                        .rawMaterialCost(prodRunResp.getRawMaterialCost())
                        .packagingCost(prodRunResp.getPackagingCost())
                        .labourCost(prodRunResp.getLabourCost())
                        .overheadCost(prodRunResp.getOverheadCost())
                        .totalProductionCost(prodRunResp.getTotalProductionCost())
                        .costPerGoodPacket(costPerGood)
                        .sellingPrice(sellingPrice)
                        .grossMarginPerPacket(marginPerPacket)
                        .grossMarginPercentage(marginPct)
                        .build())
                .auditVerifications(checks)
                .build();

        log.info("Benchmark Execution Finished. Success: {}", allPassed);
        return response;
    }

    private void addCheck(List<BenchmarkResponseDto.BenchmarkCheck> list, String name, String expected, String actual, boolean passed) {
        list.add(BenchmarkResponseDto.BenchmarkCheck.builder()
                .checkName(name)
                .expected(expected)
                .actual(actual)
                .passed(passed)
                .build());
    }

    private void assertSafeBenchmarkEnvironment() {
        if (!benchmarkEnabled) {
            throw new BusinessRuleException("Benchmark execution is disabled. Set app.benchmark.enabled=true explicitly.");
        }
        if (!environment.acceptsProfiles(Profiles.of("demo", "test"))) {
            throw new BusinessRuleException("Benchmark execution is allowed only in demo or test profiles.");
        }
        if (datasourceUrl == null || !datasourceUrl.startsWith("jdbc:h2:")) {
            throw new BusinessRuleException("Benchmark execution requires an isolated H2 database.");
        }
    }

    private void assertBusinessTablesEmpty() {
        if (productRepository.count() + customerRepository.count() + supplierRepository.count()
                + rawMaterialRepository.count() + packagingMaterialRepository.count() + bomRepository.count()
                + purchaseRepository.count() + salesOrderRepository.count() + invoiceRepository.count()
                + productionRunRepository.count() + materialConsumptionRepository.count()
                + stockMovementRepository.count() + paymentRepository.count() > 0) {
            throw new BusinessRuleException("Benchmark requires an empty isolated database; existing business records were preserved.");
        }
    }
}

