package com.ridelink.ridemanagementservice.exception;

/**
 * Thrown when a conflicting resource state is detected.
 * For example, assigning a driver to a ride that already has a driver.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
