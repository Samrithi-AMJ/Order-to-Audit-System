package com.nittrichy.ordertoaudit.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nittrichy.ordertoaudit.entity.Invoice;
import com.nittrichy.ordertoaudit.entity.InvoiceItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Component
public class PdfInvoiceGenerator {

    @Value("${app.company.name:Sam's Shop}")
    private String companyName;

    @Value("${app.company.address:Bhavani, Erode}")
    private String companyAddress;

    @Value("${app.company.gstin:33AABCS1429B1Z8}")
    private String companyGstin;

    @Value("${app.company.state:Tamil Nadu}")
    private String companyState;

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generateInvoicePdf(Invoice invoice) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        // Header Table (Company Info + TAX INVOICE title)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.addElement(new Paragraph(companyName, titleFont));
        companyCell.addElement(new Paragraph(companyAddress, subTitleFont));
        companyCell.addElement(new Paragraph("GSTIN: " + companyGstin + " | State: " + companyState + " (Code: 33)", regularFont));
        headerTable.addCell(companyCell);

        PdfPCell invoiceMetaCell = new PdfPCell();
        invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
        invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invTitle = new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(180, 40, 40)));
        invTitle.setAlignment(Element.ALIGN_RIGHT);
        invoiceMetaCell.addElement(invTitle);
        Paragraph invNo = new Paragraph("Invoice No: " + invoice.getInvoiceNumber(), boldFont);
        invNo.setAlignment(Element.ALIGN_RIGHT);
        invoiceMetaCell.addElement(invNo);
        Paragraph invDate = new Paragraph("Date: " + invoice.getInvoiceDate().format(DATE_FORMAT), regularFont);
        invDate.setAlignment(Element.ALIGN_RIGHT);
        invoiceMetaCell.addElement(invDate);
        Paragraph supplyPlace = new Paragraph("Place of Supply: " + invoice.getPlaceOfSupply(), regularFont);
        supplyPlace.setAlignment(Element.ALIGN_RIGHT);
        invoiceMetaCell.addElement(supplyPlace);
        headerTable.addCell(invoiceMetaCell);

        document.add(headerTable);
        document.add(new Paragraph(" "));

        // Bill To section
        PdfPTable billToTable = new PdfPTable(1);
        billToTable.setWidthPercentage(100);
        PdfPCell billToCell = new PdfPCell();
        billToCell.setBackgroundColor(new Color(245, 245, 245));
        billToCell.setPadding(8);
        billToCell.addElement(new Paragraph("BILLED TO:", boldFont));
        billToCell.addElement(new Paragraph(invoice.getCustomer().getName(), boldFont));
        if (invoice.getCustomer().getAddress() != null) {
            billToCell.addElement(new Paragraph(invoice.getCustomer().getAddress(), regularFont));
        }
        billToCell.addElement(new Paragraph("State: " + invoice.getCustomer().getState() +
                (invoice.getCustomerGstin() != null ? " | GSTIN: " + invoice.getCustomerGstin() : " | Unregistered"), regularFont));
        billToTable.addCell(billToCell);
        document.add(billToTable);
        document.add(new Paragraph(" "));

        // Itemized Table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 27, 10, 10, 11, 10, 13, 14});

        String[] headers = {"#", "Item Description", "HSN", "Qty", "Rate (₹)", "GST %", "Taxable (₹)", "Total (₹)"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            c.setBackgroundColor(new Color(50, 70, 90));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(5);
            table.addCell(c);
        }

        int index = 1;
        for (InvoiceItem item : invoice.getItems()) {
            PdfPCell cIdx = new PdfPCell(new Phrase(String.valueOf(index++), regularFont));
            cIdx.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cIdx);

            table.addCell(new PdfPCell(new Phrase(item.getProduct().getName(), regularFont)));

            PdfPCell cHsn = new PdfPCell(new Phrase(item.getHsnCode() != null ? item.getHsnCode() : "-", regularFont));
            cHsn.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cHsn);

            PdfPCell cQty = new PdfPCell(new Phrase(item.getQuantity().stripTrailingZeros().toPlainString(), regularFont));
            cQty.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cQty);

            PdfPCell cRate = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(item.getUnitPrice()), regularFont));
            cRate.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cRate);

            BigDecimal itemGstRate = item.getGstRate() != null ? item.getGstRate() : BigDecimal.ZERO;
            PdfPCell cGstRate = new PdfPCell(new Phrase(itemGstRate.stripTrailingZeros().toPlainString() + "%", regularFont));
            cGstRate.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cGstRate);

            PdfPCell cTaxable = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(item.getTaxableAmount()), regularFont));
            cTaxable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cTaxable);

            PdfPCell cTotal = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(item.getTotalAmount()), boldFont));
            cTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cTotal);
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // Totals & GST Summary Table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setWidths(new float[]{55, 45});

        addSummaryRow(summaryTable, "Taxable Value:", "₹ " + CURRENCY_FORMAT.format(invoice.getTaxableValue()), regularFont, regularFont);

        if (invoice.isInterState()) {
            addSummaryRow(summaryTable, "IGST (" + invoice.getIgstRate() + "%):", "₹ " + CURRENCY_FORMAT.format(invoice.getIgstAmount()), regularFont, regularFont);
        } else {
            addSummaryRow(summaryTable, "CGST (" + invoice.getCgstRate() + "%):", "₹ " + CURRENCY_FORMAT.format(invoice.getCgstAmount()), regularFont, regularFont);
            addSummaryRow(summaryTable, "SGST (" + invoice.getSgstRate() + "%):", "₹ " + CURRENCY_FORMAT.format(invoice.getSgstAmount()), regularFont, regularFont);
        }

        addSummaryRow(summaryTable, "Grand Total:", "₹ " + CURRENCY_FORMAT.format(invoice.getGrandTotal()), boldFont, boldFont);
        addSummaryRow(summaryTable, "Amount Paid:", "₹ " + CURRENCY_FORMAT.format(invoice.getPaidAmount()), regularFont, regularFont);
        addSummaryRow(summaryTable, "Outstanding Balance:", "₹ " + CURRENCY_FORMAT.format(invoice.getOutstandingAmount()), boldFont, boldFont);

        document.add(summaryTable);

        // Footer / Signature
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Paragraph terms = new Paragraph("Terms & Conditions: Goods once sold will not be taken back. Payment due within 15 days.", subTitleFont);
        document.add(terms);
        Paragraph sign = new Paragraph("For " + companyName + "\n\n\nAuthorized Signatory", boldFont);
        sign.setAlignment(Element.ALIGN_RIGHT);
        document.add(sign);

        document.close();
        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c1.setPadding(3);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(3);
        table.addCell(c2);
    }
}

