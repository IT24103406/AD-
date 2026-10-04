package com.ridelink.farepaymentservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FareEstimateResponse {
    private BigDecimal baseFare;
    private BigDecimal distanceCharge;
    private BigDecimal timeCharge;
    private BigDecimal estimatedFare;
    private String currency;
}
