package com.ridelink.ridemanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FareEstimateResponse {
    private BigDecimal baseFare;
    private BigDecimal distanceCharge;
    private BigDecimal timeCharge;
    private BigDecimal estimatedFare;
    private String currency;
}
