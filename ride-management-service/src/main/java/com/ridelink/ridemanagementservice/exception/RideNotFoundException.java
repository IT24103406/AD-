package com.ridelink.ridemanagementservice.exception;

/**
 * Thrown when a ride with the given ID is not found.
 */
public class RideNotFoundException extends RuntimeException {

    public RideNotFoundException(String message) {
        super(message);
    }

    public RideNotFoundException(String rideId, boolean isId) {
        super("Ride not found with ID: " + rideId);
    }
}
