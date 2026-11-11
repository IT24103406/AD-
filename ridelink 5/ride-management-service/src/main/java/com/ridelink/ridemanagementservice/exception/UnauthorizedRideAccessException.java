package com.ridelink.ridemanagementservice.exception;

/**
 * Thrown when an authenticated user attempts to access or modify a ride
 * they do not have ownership or permission for.
 */
public class UnauthorizedRideAccessException extends RuntimeException {

    public UnauthorizedRideAccessException(String message) {
        super(message);
    }
}
