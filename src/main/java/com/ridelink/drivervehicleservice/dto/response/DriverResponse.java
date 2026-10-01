package com.ridelink.drivervehicleservice.dto.response;

import com.ridelink.drivervehicleservice.model.Location;
import com.ridelink.drivervehicleservice.model.ServiceArea;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DriverResponse(
        String id,
        String accountId,
        String firstName,
        String lastName,
        String phone,
        String licenseNumber,
        LocalDate licenseExpiryDate,
        AvailabilityStatus availabilityStatus,
        ServiceArea serviceArea,
        Location currentLocation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
