package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.DashboardSummaryDto;
import com.nittrichy.ordertoaudit.dto.GstSummaryReportDto;
import com.nittrichy.ordertoaudit.dto.ProductionVarianceReportDto;
import com.nittrichy.ordertoaudit.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }

    @GetMapping("/gst-summary")
    public ResponseEntity<GstSummaryReportDto> getGstSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getGstSummaryReport(startDate, endDate));
    }

    @GetMapping("/production-variance")
    public ResponseEntity<List<ProductionVarianceReportDto>> getProductionVariance() {
        return ResponseEntity.ok(reportService.getProductionVarianceReports());
    }

    @GetMapping("/export/stock-ledger/excel")
    public ResponseEntity<byte[]> exportStockLedgerExcel() throws IOException {
        byte[] bytes = reportService.exportStockLedgerExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock_ledger.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export/stock-ledger/csv")
    public ResponseEntity<String> exportStockLedgerCsv() {
        String csv = reportService.exportStockLedgerCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock_ledger.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/sales-register/excel")
    public ResponseEntity<byte[]> exportSalesRegisterExcel() throws IOException {
        byte[] bytes = reportService.exportSalesRegisterExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales_register.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export/sales-register/csv")
    public ResponseEntity<String> exportSalesRegisterCsv() {
        String csv = reportService.exportSalesRegisterCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales_register.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}

