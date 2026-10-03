package com.ridelink.farepaymentservice.exception;

public class PaymentAlreadyCompletedException extends RuntimeException {
    public PaymentAlreadyCompletedException(String message) {
        super(message);
    }
}
