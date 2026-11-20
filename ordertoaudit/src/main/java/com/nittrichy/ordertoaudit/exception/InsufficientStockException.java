package com.nittrichy.ordertoaudit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStockException extends RuntimeException {
    private final String itemCode;
    private final String itemName;
    private final BigDecimal requestedQuantity;
    private final BigDecimal availableQuantity;
    private final BigDecimal shortageQuantity;

    public InsufficientStockException(String itemCode, String itemName, BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient stock for %s (%s). Requested: %s, Available: %s, Shortage: %s",
                itemName, itemCode, requested, available, requested.subtract(available)));
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.requestedQuantity = requested;
        this.availableQuantity = available;
        this.shortageQuantity = requested.subtract(available);
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public BigDecimal getShortageQuantity() {
        return shortageQuantity;
    }
}

