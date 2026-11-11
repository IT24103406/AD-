package com.ridelink.ridemanagementservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Result of an atomic driver reservation attempt in the Driver & Vehicle Service.
 * reserved = true only when the driver was atomically flipped AVAILABLE -> ON_RIDE and an
 * ACTIVE vehicle was resolved. vehicleId is present only when reserved is true.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DriverReservationResponse {

    private boolean reserved;
    private String driverId;
    private String vehicleId;
    private String reason;

    public DriverReservationResponse() {}

    public DriverReservationResponse(boolean reserved, String driverId, String vehicleId, String reason) {
        this.reserved = reserved;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.reason = reason;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
