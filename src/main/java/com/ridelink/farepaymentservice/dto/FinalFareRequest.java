package com.ridelink.farepaymentservice.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FinalFareRequest {
    @Positive(message = "Distance must be greater than 0")
    private double actualDistanceKm;
    
    @Positive(message = "Duration must be greater than 0")
    private int actualDurationMinutes;
}
