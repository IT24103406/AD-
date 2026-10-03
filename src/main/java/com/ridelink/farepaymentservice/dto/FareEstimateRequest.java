package com.ridelink.farepaymentservice.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FareEstimateRequest {
    @Positive(message = "Distance must be greater than 0")
    private double distanceKm;
    
    @Positive(message = "Duration must be greater than 0")
    private int estimatedDurationMinutes;
}
