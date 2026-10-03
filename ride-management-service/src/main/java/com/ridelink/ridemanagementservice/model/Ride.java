package com.ridelink.ridemanagementservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document representing a ride in the RideLink system.
 *
 * Only stores IDs referencing other microservices:
 * - passengerAccountId -> Account Service (ridelink_account_db)
 * - driverId           -> Driver & Vehicle Service (ridelink_driver_vehicle_db)
 * - vehicleId          -> Driver & Vehicle Service (ridelink_driver_vehicle_db)
 *
 * No cross-service DB joins are performed.
 */
@Document(collection = "rides")
public class Ride {

    @Id
    private String id;

    /** Reference to Account Service passenger account ID */
    private String passengerAccountId;

    /** Reference to Driver & Vehicle Service driver ID (null until assigned) */
    private String driverId;

    /** Reference to Driver & Vehicle Service vehicle ID (null until assigned) */
    private String vehicleId;

    private Location pickupLocation;

    private Location destinationLocation;

    private RideStatus status;

    private LocalDateTime requestedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    public Ride() {}

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

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(Location pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
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
