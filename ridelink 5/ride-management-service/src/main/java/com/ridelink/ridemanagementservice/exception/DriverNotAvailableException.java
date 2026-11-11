package com.ridelink.ridemanagementservice.exception;

/**
 * Thrown when the selected driver is not available for a ride assignment.
 */
public class DriverNotAvailableException extends RuntimeException {

    public DriverNotAvailableException(String message) {
        super(message);
    }
}
