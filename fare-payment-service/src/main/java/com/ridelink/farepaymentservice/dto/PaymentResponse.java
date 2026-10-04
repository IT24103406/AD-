package com.ridelink.farepaymentservice.dto;

import com.ridelink.farepaymentservice.model.PaymentMethod;
import com.ridelink.farepaymentservice.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private String rideId;
    private String fareId;
    private String passengerAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionReference;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
}
