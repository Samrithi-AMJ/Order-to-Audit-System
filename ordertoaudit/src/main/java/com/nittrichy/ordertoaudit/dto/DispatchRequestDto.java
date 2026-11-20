package com.nittrichy.ordertoaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchRequestDto {
    private String dispatchNotes;
    private String vehicleNumber;
}

