package com.nittrichy.ordertoaudit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderApprovalDto {
    @NotNull(message = "Approval status flag is required")
    private Boolean approved; // true to approve, false to reject

    private String rejectionReason;
}

