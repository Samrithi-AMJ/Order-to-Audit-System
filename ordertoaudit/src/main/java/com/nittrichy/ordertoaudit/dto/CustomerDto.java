package com.nittrichy.ordertoaudit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private Long id;

    @NotBlank(message = "Customer code is required")
    private String customerCode;

    @NotBlank(message = "Customer name is required")
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String address;

    @NotBlank(message = "State is required")
    private String state;

    private String stateCode;
    private String gstin;
    private BigDecimal creditLimit;
    private BigDecimal outstandingBalance;
    private Long assignedSalesmanId;
    private String assignedSalesmanName;
    private boolean active;
}

