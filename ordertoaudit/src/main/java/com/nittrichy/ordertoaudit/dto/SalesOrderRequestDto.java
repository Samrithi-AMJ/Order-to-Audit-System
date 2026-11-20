package com.nittrichy.ordertoaudit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderRequestDto {
    private String orderNumber;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private LocalDate orderDate;
    private String notes;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<SalesOrderItemDto> items;
}

