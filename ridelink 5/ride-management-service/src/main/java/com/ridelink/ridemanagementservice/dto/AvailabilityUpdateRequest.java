package com.ridelink.ridemanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for updating a driver's availability status through Driver & Vehicle Service.
 * Supports both 'status' (Member 2 API) and 'availabilityStatus'.
 */
public class AvailabilityUpdateRequest {

    @JsonProperty("status")
    private String status;

    public AvailabilityUpdateRequest() {}

    public AvailabilityUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("availabilityStatus")
    public String getAvailabilityStatus() {
        return status;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.status = availabilityStatus;
    }
}
