package com.ridelink.farepaymentservice.dto;

import com.ridelink.farepaymentservice.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotBlank(message = "Ride ID must not be blank")
    private String rideId;
    
    @NotNull(message = "Payment method must not be null")
    private PaymentMethod paymentMethod;
}
