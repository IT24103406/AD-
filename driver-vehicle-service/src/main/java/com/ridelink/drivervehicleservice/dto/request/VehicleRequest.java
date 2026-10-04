package com.ridelink.drivervehicleservice.dto.request;

import com.ridelink.drivervehicleservice.model.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VehicleRequest(
        @NotBlank(message = "registrationNumber must not be blank")
        String registrationNumber,

        @NotBlank(message = "make must not be blank")
        String make,

        @NotBlank(message = "model must not be blank")
        String model,

        @NotNull(message = "vehicleType must not be null")
        VehicleType vehicleType,

        String color,

        @Positive(message = "manufactureYear must be positive")
        Integer manufactureYear,

        @Positive(message = "seatingCapacity must be positive")
        Integer seatingCapacity
) {}
