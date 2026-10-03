package com.ridelink.ridemanagementservice.exception;

/**
 * Thrown when an invalid ride state transition is attempted.
 * For example: trying to start a ride that is not yet ACCEPTED.
 */
public class InvalidRideStateException extends RuntimeException {

    public InvalidRideStateException(String message) {
        super(message);
    }
}
