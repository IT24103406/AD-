package com.ridelink.ridemanagementservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new ride request.
 *
 * The passengerAccountId is derived from the authenticated JWT token in the service layer.
 * Clients must NOT supply a different passengerAccountId unless they are ADMIN.
 */
public class CreateRideRequest {

    @NotNull(message = "Pickup location is required")
    @Valid
    private LocationRequest pickupLocation;

    @NotNull(message = "Destination location is required")
    @Valid
    private LocationRequest destinationLocation;

    public CreateRideRequest() {}

    public CreateRideRequest(LocationRequest pickupLocation, LocationRequest destinationLocation) {
        this.pickupLocation = pickupLocation;
        this.destinationLocation = destinationLocation;
    }

    public LocationRequest getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(LocationRequest pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public LocationRequest getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(LocationRequest destinationLocation) {
        this.destinationLocation = destinationLocation;
    }
}
