package com.ridelink.farepaymentservice.controller;

import com.ridelink.farepaymentservice.dto.FareEstimateRequest;
import com.ridelink.farepaymentservice.dto.FareEstimateResponse;
import com.ridelink.farepaymentservice.dto.FareResponse;
import com.ridelink.farepaymentservice.dto.FinalFareRequest;
import com.ridelink.farepaymentservice.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
@Tag(name = "Fare API", description = "Endpoints for fare estimation and calculation")
@SecurityRequirement(name = "bearerAuth")
public class FareController {

    private final FareService fareService;

    @PostMapping("/estimate")
    @Operation(summary = "Estimate fare before a ride is completed")
    public ResponseEntity<FareEstimateResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.estimateFare(request));
    }

    @PostMapping("/rides/{rideId}/final")
    @Operation(summary = "Calculate and persist final fare for a completed ride")
    public ResponseEntity<FareResponse> calculateFinalFare(
            @PathVariable String rideId,
            @Valid @RequestBody FinalFareRequest request) {
        return new ResponseEntity<>(fareService.calculateFinalFare(rideId, request), HttpStatus.CREATED);
    }

    @GetMapping("/rides/{rideId}")
    @Operation(summary = "Get final fare by ride ID")
    public ResponseEntity<FareResponse> getFareByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(fareService.getFareByRideId(rideId));
    }
}
