package com.ridelink.ridemanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
