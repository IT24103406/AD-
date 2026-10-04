package com.ridelink.drivervehicleservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(
        @NotNull(message = "status must not be null")
        @JsonAlias("availabilityStatus")
        AvailabilityStatus status
) {}
