package com.ridelink.drivervehicleservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ServiceAreaRequest(
        @NotBlank(message = "city must not be blank")
        String city,

        @NotBlank(message = "district must not be blank")
        String district
) {}
