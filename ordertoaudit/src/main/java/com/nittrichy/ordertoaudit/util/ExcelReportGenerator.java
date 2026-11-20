package com.nittrichy.ordertoaudit.util;

import com.nittrichy.ordertoaudit.dto.GstSummaryReportDto;
import com.nittrichy.ordertoaudit.dto.StockMovementDto;
import com.nittrichy.ordertoaudit.entity.Invoice;
import com.nittrichy.ordertoaudit.entity.Purchase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ExcelReportGenerator {

    public byte[] generateStockLedgerExcel(List<StockMovementDto> movements) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Stock Movement Ledger");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] columns = {"Movement Code", "Date & Time", "Item Type", "Item Code", "Item Name", "Movement Type",
                    "Opening Stock", "Quantity Change", "Closing Stock", "Unit", "Reference Type", "Reference No", "Remarks"};

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (StockMovementDto sm : movements) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sm.getMovementCode());
                row.createCell(1).setCellValue(sm.getMovementDate() != null ? sm.getMovementDate().toString() : "");
                row.createCell(2).setCellValue(sm.getItemType() != null ? sm.getItemType().name() : "");
                row.createCell(3).setCellValue(sm.getItemCode());
                row.createCell(4).setCellValue(sm.getItemName());
                row.createCell(5).setCellValue(sm.getMovementType() != null ? sm.getMovementType().name() : "");
                row.createCell(6).setCellValue(sm.getOpeningStock() != null ? sm.getOpeningStock().doubleValue() : 0.0);
                row.createCell(7).setCellValue(sm.getQuantity() != null ? sm.getQuantity().doubleValue() : 0.0);
                row.createCell(8).setCellValue(sm.getClosingStock() != null ? sm.getClosingStock().doubleValue() : 0.0);
                row.createCell(9).setCellValue(sm.getUnit());
                row.createCell(10).setCellValue(sm.getReferenceType() != null ? sm.getReferenceType().name() : "");
                row.createCell(11).setCellValue(sm.getReferenceNumber() != null ? sm.getReferenceNumber() : "");
                row.createCell(12).setCellValue(sm.getRemarks() != null ? sm.getRemarks() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateSalesReportExcel(List<Invoice> invoices) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Register");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columns = {"Invoice No", "Date", "Customer", "State", "GSTIN", "Taxable Value (₹)",
                    "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total (₹)", "Paid (₹)", "Outstanding (₹)", "Status"};

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(inv.getInvoiceNumber());
                row.createCell(1).setCellValue(inv.getInvoiceDate().toString());
                row.createCell(2).setCellValue(inv.getCustomer().getName());
                row.createCell(3).setCellValue(inv.getPlaceOfSupply());
                row.createCell(4).setCellValue(inv.getCustomerGstin() != null ? inv.getCustomerGstin() : "Unregistered");
                row.createCell(5).setCellValue(inv.getTaxableValue().doubleValue());
                row.createCell(6).setCellValue(inv.getCgstAmount().doubleValue());
                row.createCell(7).setCellValue(inv.getSgstAmount().doubleValue());
                row.createCell(8).setCellValue(inv.getIgstAmount().doubleValue());
                row.createCell(9).setCellValue(inv.getGrandTotal().doubleValue());
                row.createCell(10).setCellValue(inv.getPaidAmount().doubleValue());
                row.createCell(11).setCellValue(inv.getOutstandingAmount().doubleValue());
                row.createCell(12).setCellValue(inv.getStatus().name());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}

