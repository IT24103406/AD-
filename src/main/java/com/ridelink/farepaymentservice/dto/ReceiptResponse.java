package com.ridelink.farepaymentservice.dto;

import com.ridelink.farepaymentservice.model.PaymentMethod;
import com.ridelink.farepaymentservice.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReceiptResponse {
    private String receiptNumber;
    private String transactionReference;
    private String rideId;
    private String passengerAccountId;
    private BigDecimal fareAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String currency;
    private LocalDateTime paidAt;
}
