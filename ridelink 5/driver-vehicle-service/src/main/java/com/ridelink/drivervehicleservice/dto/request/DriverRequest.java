package com.ridelink.drivervehicleservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DriverRequest(
        @NotBlank(message = "accountId must not be blank")
        String accountId,

        String firstName,
        String lastName,
        String phone,

        @NotBlank(message = "licenseNumber must not be blank")
        String licenseNumber,

        @NotNull(message = "licenseExpiryDate must not be null")
        LocalDate licenseExpiryDate
) {}
