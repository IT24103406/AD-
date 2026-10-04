package com.ridelink.ridemanagementservice.dto;

import com.ridelink.ridemanagementservice.model.RideStatus;

import java.time.LocalDateTime;

/**
 * DTO for ride API responses. Does not expose internal MongoDB entity directly.
 */
public class RideResponse {

    private String id;
    private String passengerAccountId;
    private String driverId;
    private String vehicleId;
    private LocationResponse pickupLocation;
    private LocationResponse destinationLocation;
    private RideStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    public RideResponse() {}

    // ---- Getters and Setters ----

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassengerAccountId() {
        return passengerAccountId;
    }

    public void setPassengerAccountId(String passengerAccountId) {
        this.passengerAccountId = passengerAccountId;
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

    public LocationResponse getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(LocationResponse pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public LocationResponse getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(LocationResponse destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
