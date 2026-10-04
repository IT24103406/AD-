package com.ridelink.farepaymentservice.exception;

public class FareNotFoundException extends RuntimeException {
    public FareNotFoundException(String message) {
        super(message);
    }
}
