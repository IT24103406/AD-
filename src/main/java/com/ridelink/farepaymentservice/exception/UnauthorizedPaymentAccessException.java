package com.ridelink.farepaymentservice.exception;

public class UnauthorizedPaymentAccessException extends RuntimeException {
    public UnauthorizedPaymentAccessException(String message) {
        super(message);
    }
}
