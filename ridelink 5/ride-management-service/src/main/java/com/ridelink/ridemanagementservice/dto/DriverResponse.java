package com.ridelink.ridemanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

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

    /** When the driver last became AVAILABLE; used for longest-available-first ordering */
    private LocalDateTime availableSince;

    /** An ACTIVE vehicle belonging to this driver, resolved by Driver Service (eligible queries only) */
    private String activeVehicleId;

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

    public LocalDateTime getAvailableSince() {
        return availableSince;
    }

    public void setAvailableSince(LocalDateTime availableSince) {
        this.availableSince = availableSince;
    }

    public String getActiveVehicleId() {
        return activeVehicleId;
    }

    public void setActiveVehicleId(String activeVehicleId) {
        this.activeVehicleId = activeVehicleId;
    }
}
