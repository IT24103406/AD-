package com.ridelink.drivervehicleservice.dto.response;

import com.ridelink.drivervehicleservice.model.enums.VehicleStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleType;

import java.time.LocalDateTime;

public record VehicleResponse(
        String id,
        String driverId,
        String registrationNumber,
        String make,
        String model,
        VehicleType vehicleType,
        String color,
        Integer manufactureYear,
        Integer seatingCapacity,
        VehicleStatus vehicleStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
