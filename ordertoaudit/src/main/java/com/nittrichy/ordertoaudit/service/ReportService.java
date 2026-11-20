package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.*;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.ProductionStatus;
import com.nittrichy.ordertoaudit.repository.*;
import com.nittrichy.ordertoaudit.util.CsvReportGenerator;
import com.nittrichy.ordertoaudit.util.ExcelReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductionRunRepository productionRunRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryService inventoryService;
    private final ExcelReportGenerator excelReportGenerator;
    private final CsvReportGenerator csvReportGenerator;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        List<Invoice> invoices = invoiceRepository.findAll();
        BigDecimal totalSales = invoices.stream()
                .map(Invoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceivables = customerRepository.findAll().stream()
                .map(c -> c.getOutstandingBalance() != null ? c.getOutstandingBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPurchases = purchaseRepository.findAll().stream()
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = salesOrderRepository.findAll().stream()
                .filter(so -> so.getStatus() == com.nittrichy.ordertoaudit.enums.OrderStatus.PENDING_APPROVAL)
                .count();

        long activeRuns = productionRunRepository.findAll().stream()
                .filter(pr -> pr.getStatus() == ProductionStatus.IN_PROGRESS)
                .count();

        List<StockSummaryDto> allStock = inventoryService.getAllStockSummaries();
        long lowStockCount = allStock.stream().filter(StockSummaryDto::isLowStock).count();

        BigDecimal rmValuation = allStock.stream()
                .filter(s -> s.getItemType() == ItemType.RAW_MATERIAL)
                .map(StockSummaryDto::getTotalValuation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pmValuation = allStock.stream()
                .filter(s -> s.getItemType() == ItemType.PACKAGING_MATERIAL)
                .map(StockSummaryDto::getTotalValuation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fgValuation = allStock.stream()
                .filter(s -> s.getItemType() == ItemType.FINISHED_GOODS)
                .map(StockSummaryDto::getTotalValuation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categorySales = new HashMap<>();
        for (Invoice inv : invoices) {
            for (InvoiceItem item : inv.getItems()) {
                String cat = item.getProduct().getCategory().name();
                categorySales.put(cat, categorySales.getOrDefault(cat, BigDecimal.ZERO).add(item.getTotalAmount()));
            }
        }

        return DashboardSummaryDto.builder()
                .totalSales(totalSales)
                .totalReceivables(totalReceivables)
                .totalPurchases(totalPurchases)
                .pendingOrdersCount(pendingOrders)
                .activeProductionRunsCount(activeRuns)
                .lowStockAlertsCount(lowStockCount)
                .rawMaterialStockValuation(rmValuation)
                .packagingStockValuation(pmValuation)
                .finishedGoodsStockValuation(fgValuation)
                .categoryWiseSales(categorySales)
                .build();
    }

    @Transactional(readOnly = true)
    public GstSummaryReportDto getGstSummaryReport(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices;
        if (startDate != null && endDate != null) {
            invoices = invoiceRepository.findByInvoiceDateBetween(startDate, endDate);
        } else {
            invoices = invoiceRepository.findAll();
        }

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;

        List<GstSummaryReportDto.GstLineItemDto> lineItems = new ArrayList<>();

        for (Invoice inv : invoices) {
            totalTaxable = totalTaxable.add(inv.getTaxableValue());
            totalCgst = totalCgst.add(inv.getCgstAmount());
            totalSgst = totalSgst.add(inv.getSgstAmount());
            totalIgst = totalIgst.add(inv.getIgstAmount());
            totalGross = totalGross.add(inv.getGrandTotal());

            lineItems.add(GstSummaryReportDto.GstLineItemDto.builder()
                    .invoiceNumber(inv.getInvoiceNumber())
                    .invoiceDate(inv.getInvoiceDate().toString())
                    .customerName(inv.getCustomer().getName())
                    .customerGstin(inv.getCustomerGstin())
                    .placeOfSupply(inv.getPlaceOfSupply())
                    .isInterState(inv.isInterState())
                    .taxableValue(inv.getTaxableValue())
                    .cgstAmount(inv.getCgstAmount())
                    .sgstAmount(inv.getSgstAmount())
                    .igstAmount(inv.getIgstAmount())
                    .invoiceTotal(inv.getGrandTotal())
                    .build());
        }

        BigDecimal totalGst = totalCgst.add(totalSgst).add(totalIgst);

        return GstSummaryReportDto.builder()
                .financialYear("2026-27")
                .totalTaxableValue(totalTaxable)
                .totalCgst(totalCgst)
                .totalSgst(totalSgst)
                .totalIgst(totalIgst)
                .totalGstCollected(totalGst)
                .totalGrossInvoiced(totalGross)
                .lineItems(lineItems)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductionVarianceReportDto> getProductionVarianceReports() {
        return productionRunRepository.findAll().stream().map(run -> {
            List<ProductionVarianceReportDto.MaterialVarianceItemDto> items = run.getConsumptions().stream()
                    .map(c -> {
                        String name = c.getRawMaterial() != null ? c.getRawMaterial().getName() : (c.getPackagingMaterial() != null ? c.getPackagingMaterial().getName() : "");
                        return ProductionVarianceReportDto.MaterialVarianceItemDto.builder()
                                .materialName(name)
                                .materialType(c.getItemType().name())
                                .plannedQuantity(c.getPlannedQuantity())
                                .actualQuantity(c.getActualQuantity())
                                .damagedQuantity(c.getDamagedQuantity())
                                .variancePercentage(c.getVariancePercentage())
                                .flagged(c.isVarianceFlag())
                                .totalCost(c.getTotalCost())
                                .build();
                    }).collect(Collectors.toList());

            return ProductionVarianceReportDto.builder()
                    .runNumber(run.getRunNumber())
                    .productName(run.getProduct().getName())
                    .runDate(run.getRunDate().toString())
                    .expectedOutput(run.getExpectedOutput())
                    .actualGoodOutput(run.getActualGoodOutput())
                    .yieldVariancePercentage(run.getYieldVariancePercentage())
                    .yieldVarianceFlagged(run.isVarianceFlag())
                    .totalCost(run.getTotalProductionCost())
                    .costPerPacket(run.getCostPerPacket())
                    .items(items)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] exportStockLedgerExcel() throws IOException {
        List<StockMovementDto> movements = inventoryService.getStockLedger(null, null, null);
        return excelReportGenerator.generateStockLedgerExcel(movements);
    }

    @Transactional(readOnly = true)
    public String exportStockLedgerCsv() {
        List<StockMovementDto> movements = inventoryService.getStockLedger(null, null, null);
        return csvReportGenerator.generateStockLedgerCsv(movements);
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesRegisterExcel() throws IOException {
        List<Invoice> invoices = invoiceRepository.findAll();
        return excelReportGenerator.generateSalesReportExcel(invoices);
    }

    @Transactional(readOnly = true)
    public String exportSalesRegisterCsv() {
        List<Invoice> invoices = invoiceRepository.findAll();
        return csvReportGenerator.generateSalesRegisterCsv(invoices);
    }
}

