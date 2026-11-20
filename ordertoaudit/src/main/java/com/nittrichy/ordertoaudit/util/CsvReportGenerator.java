package com.nittrichy.ordertoaudit.util;

import com.nittrichy.ordertoaudit.dto.StockMovementDto;
import com.nittrichy.ordertoaudit.entity.Invoice;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.List;

@Component
public class CsvReportGenerator {

    public String generateStockLedgerCsv(List<StockMovementDto> movements) {
        StringWriter writer = new StringWriter();
        writer.write("MovementCode,Date,ItemType,ItemCode,ItemName,MovementType,OpeningStock,Quantity,ClosingStock,Unit,ReferenceType,ReferenceNo,Remarks\n");

        for (StockMovementDto sm : movements) {
            writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    escape(sm.getMovementCode()),
                    sm.getMovementDate() != null ? sm.getMovementDate().toString() : "",
                    sm.getItemType() != null ? sm.getItemType().name() : "",
                    escape(sm.getItemCode()),
                    escape(sm.getItemName()),
                    sm.getMovementType() != null ? sm.getMovementType().name() : "",
                    sm.getOpeningStock(),
                    sm.getQuantity(),
                    sm.getClosingStock(),
                    escape(sm.getUnit()),
                    sm.getReferenceType() != null ? sm.getReferenceType().name() : "",
                    escape(sm.getReferenceNumber()),
                    escape(sm.getRemarks())
            ));
        }

        return writer.toString();
    }

    public String generateSalesRegisterCsv(List<Invoice> invoices) {
        StringWriter writer = new StringWriter();
        writer.write("InvoiceNo,Date,Customer,State,GSTIN,TaxableValue,CGST,SGST,IGST,GrandTotal,Paid,Outstanding,Status\n");

        for (Invoice inv : invoices) {
            writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s,%s,%s,\"%s\"\n",
                    escape(inv.getInvoiceNumber()),
                    inv.getInvoiceDate(),
                    escape(inv.getCustomer().getName()),
                    escape(inv.getPlaceOfSupply()),
                    escape(inv.getCustomerGstin()),
                    inv.getTaxableValue(),
                    inv.getCgstAmount(),
                    inv.getSgstAmount(),
                    inv.getIgstAmount(),
                    inv.getGrandTotal(),
                    inv.getPaidAmount(),
                    inv.getOutstandingAmount(),
                    inv.getStatus().name()
            ));
        }

        return writer.toString();
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}

