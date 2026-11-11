package com.ridelink.ridemanagementservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for assigning a driver and vehicle to a ride.
 *
 * NOTE: The exact field names (driverId, vehicleId) must match what is stored
 * in the Driver & Vehicle Service. Adapt if Member 2 uses different field names.
 */
public class AssignDriverRequest {

    @NotBlank(message = "Driver ID must not be blank")
    private String driverId;

    @NotBlank(message = "Vehicle ID must not be blank")
    private String vehicleId;

    public AssignDriverRequest() {}

    public AssignDriverRequest(String driverId, String vehicleId) {
        this.driverId = driverId;
        this.vehicleId = vehicleId;
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
}
