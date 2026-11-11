package com.ridelink.ridemanagementservice.exception;

/**
 * Thrown when communication with an external service (Driver Service, Fare Service) fails.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
