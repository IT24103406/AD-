package com.ridelink.farepaymentservice.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "fares")
public class Fare {
    @Id
    private String id;
    @Indexed(unique = true)
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
