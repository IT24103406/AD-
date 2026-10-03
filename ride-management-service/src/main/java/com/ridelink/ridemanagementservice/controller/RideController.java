package com.ridelink.ridemanagementservice.controller;

import com.ridelink.ridemanagementservice.dto.AssignDriverRequest;
import com.ridelink.ridemanagementservice.dto.CancelRideRequest;
import com.ridelink.ridemanagementservice.dto.CreateRideRequest;
import com.ridelink.ridemanagementservice.dto.RideResponse;
import com.ridelink.ridemanagementservice.model.RideStatus;
import com.ridelink.ridemanagementservice.security.AuthenticatedUserService;
import com.ridelink.ridemanagementservice.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Ride Management endpoints.
 *
 * All endpoints require JWT Bearer authentication.
 * Role-based access control is enforced here and in the service layer.
 */
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Management", description = "APIs for managing ride lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;
    private final AuthenticatedUserService authenticatedUserService;

    public RideController(RideService rideService, AuthenticatedUserService authenticatedUserService) {
        this.rideService = rideService;
        this.authenticatedUserService = authenticatedUserService;
    }

    // ================================================================
    // POST /api/rides - Create a new ride
    // ================================================================

    @PostMapping
    @Operation(
        summary = "Create a ride request",
        description = "Create a new ride request. Only PASSENGER role can create rides. " +
                      "The passenger identity is derived from the JWT token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ride created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not a passenger")
    })
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        String passengerAccountId = authenticatedUserService.getCurrentAccountId();
        RideResponse response = rideService.createRide(request, passengerAccountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // GET /api/rides/{rideId} - Get ride details
    // ================================================================

    @GetMapping("/{rideId}")
    @Operation(
        summary = "Get ride by ID",
        description = "Retrieve details of a specific ride. ADMIN can view any ride. " +
                      "Passengers and drivers can view their own rides."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    public ResponseEntity<RideResponse> getRideById(
            @PathVariable @Parameter(description = "Ride ID") String rideId) {
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // PUT /api/rides/{rideId}/assign-driver
    // ================================================================

    @PutMapping("/{rideId}/assign-driver")
    @Operation(
        summary = "Assign driver to ride",
        description = "Assign a driver and vehicle to a REQUESTED ride. " +
                      "Only ADMIN role can assign drivers."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Driver assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not in REQUESTED state or driver not available")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RideResponse> assignDriver(
            @PathVariable String rideId,
            @Valid @RequestBody AssignDriverRequest request) {
        String requestingAccountId = authenticatedUserService.getCurrentAccountId();
        RideResponse response = rideService.assignDriver(rideId, request, requestingAccountId);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // PUT /api/rides/{rideId}/accept - Driver accepts ride
    // ================================================================

    @PutMapping("/{rideId}/accept")
    @Operation(
        summary = "Accept an assigned ride",
        description = "Driver accepts a ride that has been assigned to them. " +
                      "Only DRIVER role can accept rides."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride accepted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden or ride not assigned to this driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not in DRIVER_ASSIGNED state")
    })
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable String rideId) {
        String driverAccountId = authenticatedUserService.getCurrentAccountId();
        RideResponse response = rideService.acceptRide(rideId, driverAccountId);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // PUT /api/rides/{rideId}/start - Driver starts ride
    // ================================================================

    @PutMapping("/{rideId}/start")
    @Operation(
        summary = "Start an accepted ride",
        description = "Driver starts a ride that is in ACCEPTED state. " +
                      "Only DRIVER role can start rides."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride started"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden or ride not assigned to this driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not in ACCEPTED state")
    })
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> startRide(@PathVariable String rideId) {
        String driverAccountId = authenticatedUserService.getCurrentAccountId();
        RideResponse response = rideService.startRide(rideId, driverAccountId);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // PUT /api/rides/{rideId}/complete - Driver completes ride
    // ================================================================

    @PutMapping("/{rideId}/complete")
    @Operation(
        summary = "Complete an in-progress ride",
        description = "Driver completes a ride that is IN_PROGRESS. " +
                      "Only DRIVER role can complete rides."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden or ride not assigned to this driver"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride is not IN_PROGRESS")
    })
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String rideId) {
        String driverAccountId = authenticatedUserService.getCurrentAccountId();
        RideResponse response = rideService.completeRide(rideId, driverAccountId);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // PUT /api/rides/{rideId}/cancel
    // ================================================================

    @PutMapping("/{rideId}/cancel")
    @Operation(
        summary = "Cancel a ride",
        description = "Cancel a ride. PASSENGER can cancel their own ride. " +
                      "DRIVER can cancel rides assigned to them. ADMIN can cancel any ride. " +
                      "Cannot cancel COMPLETED or IN_PROGRESS rides."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride cancelled"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Ride not found"),
        @ApiResponse(responseCode = "409", description = "Ride cannot be cancelled in current state")
    })
    @PreAuthorize("hasAnyRole('PASSENGER', 'DRIVER', 'ADMIN')")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable String rideId,
            @Valid @RequestBody(required = false) CancelRideRequest request) {
        String accountId = authenticatedUserService.getCurrentAccountId();
        String role = authenticatedUserService.getCurrentRole();
        RideResponse response = rideService.cancelRide(rideId, request, accountId, role);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // GET /api/rides/passenger/{passengerAccountId}
    // ================================================================

    @GetMapping("/passenger/{passengerAccountId}")
    @Operation(
        summary = "Get passenger ride history",
        description = "Retrieve all rides for a specific passenger. " +
                      "Passengers can only view their own history. ADMIN can view any."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride history returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - can only view own history")
    })
    @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
    public ResponseEntity<List<RideResponse>> getPassengerRides(
            @PathVariable String passengerAccountId) {
        String requestingAccountId = authenticatedUserService.getCurrentAccountId();
        String requestingRole = authenticatedUserService.getCurrentRole();
        List<RideResponse> rides = rideService.getPassengerRides(passengerAccountId, requestingAccountId, requestingRole);
        return ResponseEntity.ok(rides);
    }

    // ================================================================
    // GET /api/rides/driver/{driverId}
    // ================================================================

    @GetMapping("/driver/{driverId}")
    @Operation(
        summary = "Get driver ride history",
        description = "Retrieve all rides for a specific driver. " +
                      "Drivers can view their own history. ADMIN can view any."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Driver ride history returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<List<RideResponse>> getDriverRides(@PathVariable String driverId) {
        String requestingAccountId = authenticatedUserService.getCurrentAccountId();
        String requestingRole = authenticatedUserService.getCurrentRole();
        List<RideResponse> rides = rideService.getDriverRides(driverId, requestingAccountId, requestingRole);
        return ResponseEntity.ok(rides);
    }

    // ================================================================
    // GET /api/rides/status/{status}
    // ================================================================

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get rides by status",
        description = "Retrieve all rides with a specific status. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rides returned"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN only")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RideResponse>> getRidesByStatus(
            @PathVariable @Parameter(description = "Ride status: REQUESTED, DRIVER_ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED")
            RideStatus status) {
        List<RideResponse> rides = rideService.getRidesByStatus(status);
        return ResponseEntity.ok(rides);
    }
}
