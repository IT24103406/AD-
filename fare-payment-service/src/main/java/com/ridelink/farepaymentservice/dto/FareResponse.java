package com.ridelink.farepaymentservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FareResponse {
    private String id;
    private String rideId;
    private String passengerAccountId;
    private BigDecimal baseFare;
    private double distanceKm;
    private int durationMinutes;
    private BigDecimal distanceCharge;
    private BigDecimal timeCharge;
    private BigDecimal totalFare;
    private String currency;
    private LocalDateTime calculatedAt;
}
