package com.ridelink.drivervehicleservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull(message = "latitude must not be null")
        @Min(value = -90, message = "latitude must be between -90 and 90")
        @Max(value = 90, message = "latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "longitude must not be null")
        @Min(value = -180, message = "longitude must be between -180 and 180")
        @Max(value = 180, message = "longitude must be between -180 and 180")
        Double longitude
) {}
