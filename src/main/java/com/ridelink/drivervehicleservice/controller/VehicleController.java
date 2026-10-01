package com.ridelink.drivervehicleservice.controller;

import com.ridelink.drivervehicleservice.dto.request.VehicleRequest;
import com.ridelink.drivervehicleservice.dto.response.VehicleResponse;
import com.ridelink.drivervehicleservice.service.VehicleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @PostMapping("/drivers/{driverId}/vehicles")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<VehicleResponse> addVehicle(@PathVariable String driverId, @Valid @RequestBody VehicleRequest request) {
        return new ResponseEntity<>(vehicleService.addVehicle(driverId, request), HttpStatus.CREATED);
    }

    @GetMapping("/drivers/{driverId}/vehicles")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByDriver(@PathVariable String driverId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByDriver(driverId));
    }

    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable String vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicle(vehicleId));
    }

    @PutMapping("/vehicles/{vehicleId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable String vehicleId, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(vehicleId, request));
    }
}
