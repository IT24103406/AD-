package com.ridelink.farepaymentservice.exception;

public class InvalidRideStateException extends RuntimeException {
    public InvalidRideStateException(String message) {
        super(message);
    }
}
