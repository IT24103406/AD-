package com.ridelink.farepaymentservice.service;

import com.ridelink.farepaymentservice.dto.PaymentRequest;
import com.ridelink.farepaymentservice.dto.PaymentResponse;
import com.ridelink.farepaymentservice.dto.ReceiptResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getPaymentById(String paymentId);
    PaymentResponse getPaymentByRideId(String rideId);
    ReceiptResponse generateReceipt(String paymentId);
}
