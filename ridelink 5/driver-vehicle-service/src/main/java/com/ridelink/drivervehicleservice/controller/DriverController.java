package com.ridelink.drivervehicleservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.drivervehicleservice.dto.request.AvailabilityRequest;
import com.ridelink.drivervehicleservice.dto.request.DriverRequest;
import com.ridelink.drivervehicleservice.dto.request.LocationRequest;
import com.ridelink.drivervehicleservice.dto.request.ServiceAreaRequest;
import com.ridelink.drivervehicleservice.dto.response.DriverReservationResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverResponse;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import com.ridelink.drivervehicleservice.service.DriverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/drivers")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Create driver profile",
            description = "Creates a driver profile after validating the account via Account Service. Requires DRIVER JWT."
    )
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody DriverRequest request) {
        return new ResponseEntity<>(driverService.createDriver(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List all drivers (admin)",
            description = "Retrieve all registered drivers. Optionally filter by availability status. ADMIN only."
    )
    public ResponseEntity<List<DriverResponse>> getAllDrivers(
            @RequestParam(required = false) AvailabilityStatus availabilityStatus) {
        return ResponseEntity.ok(driverService.getAllDrivers(availabilityStatus));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver by id or accountId")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable String id) {
        return ResponseEntity.ok(driverService.getDriver(id));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get driver by account id")
    public ResponseEntity<DriverResponse> getDriverByAccountId(@PathVariable String accountId) {
        return ResponseEntity.ok(driverService.getDriverByAccountId(accountId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateDriver(@PathVariable String id, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(id, request));
    }

    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateAvailability(@PathVariable String id, @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(id, request));
    }

    @PutMapping("/{id}/service-area")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateServiceArea(@PathVariable String id, @Valid @RequestBody ServiceAreaRequest request) {
        return ResponseEntity.ok(driverService.updateServiceArea(id, request));
    }

    @PutMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateLocation(@PathVariable String id, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(driverService.updateLocation(id, request));
    }

    @GetMapping("/eligible")
    @SecurityRequirements // public endpoint — clear inherited bearer requirement for Swagger
    @Operation(summary = "List eligible drivers (public)",
            description = "AVAILABLE drivers with an ACTIVE vehicle, optionally filtered by service area " +
                    "(matches serviceArea.city OR serviceArea.district, case-insensitive) and ordered " +
                    "longest-available first. `serviceArea` takes precedence over the legacy `city` param.")
    public ResponseEntity<List<DriverResponse>> getEligibleDrivers(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) String vehicleType) {
        String area = (serviceArea != null && !serviceArea.isBlank()) ? serviceArea : city;
        return ResponseEntity.ok(driverService.getEligibleDrivers(area, vehicleType));
    }

    @PostMapping("/{id}/reserve")
    @Operation(summary = "Atomically reserve a driver for a ride",
            description = "Conditionally flips the driver AVAILABLE -> ON_RIDE in a single atomic " +
                    "operation so concurrent requests cannot reserve the same driver. Resolves an ACTIVE " +
                    "vehicle; if none exists the driver is released back to AVAILABLE. Returns reserved=true " +
                    "with the vehicleId on success.")
    public ResponseEntity<DriverReservationResponse> reserveDriver(
            @PathVariable String id,
            @RequestParam(required = false) String vehicleType) {
        return ResponseEntity.ok(driverService.reserveDriver(id, vehicleType));
    }
}
