package com.ridelink.ridemanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the driver data returned by the Driver & Vehicle Service.
 *
 * Field names MUST match the actual JSON from Driver Service.
 * Verify against Driver Service Swagger at: http://localhost:8082/swagger-ui/index.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DriverResponse {

    /** Driver entity ID in Driver & Vehicle Service */
    private String id;

    /** Account ID linking to Account Service */
    private String accountId;

    /** Availability status: AVAILABLE, UNAVAILABLE, ON_RIDE */
    private String availabilityStatus;

    public DriverResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
