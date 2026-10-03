package com.ridelink.ridemanagementservice.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for cancelling a ride with an optional reason.
 */
public class CancelRideRequest {

    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String reason;

    public CancelRideRequest() {}

    public CancelRideRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
