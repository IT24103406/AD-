package com.ridelink.ridemanagementservice.service;

import com.ridelink.ridemanagementservice.dto.*;
import com.ridelink.ridemanagementservice.exception.*;
import com.ridelink.ridemanagementservice.model.Location;
import com.ridelink.ridemanagementservice.model.Ride;
import com.ridelink.ridemanagementservice.model.RideStatus;
import com.ridelink.ridemanagementservice.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of RideService.
 *
 * All business logic lives here:
 * - Ride lifecycle state transitions
 * - Ownership validation
 * - Driver service integration via DriverServiceClient
 */
@Service
public class RideServiceImpl implements RideService {

    private static final Logger log = LoggerFactory.getLogger(RideServiceImpl.class);

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;

    public RideServiceImpl(RideRepository rideRepository, DriverServiceClient driverServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
    }

    // ================================================================
    // CREATE RIDE
    // ================================================================

    @Override
    public RideResponse createRide(CreateRideRequest request, String passengerAccountId) {
        Ride ride = new Ride();
        ride.setPassengerAccountId(passengerAccountId);
        ride.setPickupLocation(mapToLocation(request.getPickupLocation()));
        ride.setDestinationLocation(mapToLocation(request.getDestinationLocation()));
        ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedAt(LocalDateTime.now());

        Ride saved = rideRepository.save(ride);
        log.info("Created ride '{}' for passenger '{}'", saved.getId(), passengerAccountId);
        return mapToResponse(saved);
    }

    // ================================================================
    // GET RIDE BY ID
    // ================================================================

    @Override
    public RideResponse getRideById(String rideId) {
        Ride ride = findRideOrThrow(rideId);
        return mapToResponse(ride);
    }

    // ================================================================
    // ASSIGN DRIVER
    // ================================================================

    @Override
    public RideResponse assignDriver(String rideId, AssignDriverRequest request, String requestingAccountId) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException(
                "Cannot assign a driver to ride in status: " + ride.getStatus() +
                ". Driver can only be assigned when ride is REQUESTED.");
        }

        // Validate driver exists and is available via Driver Service REST API
        DriverResponse driver = driverServiceClient.getDriverById(request.getDriverId());
        if (driver == null) {
            driver = driverServiceClient.getDriverByAccountId(request.getDriverId());
        }

        if (driver == null) {
            throw new DriverNotAvailableException("Driver not found: " + request.getDriverId());
        }

        String availability = driver.getAvailabilityStatus();
        if (!"AVAILABLE".equalsIgnoreCase(availability)) {
            throw new DriverNotAvailableException(
                "Driver '" + request.getDriverId() + "' is not available. Current status: " + availability);
        }

        String resolvedDriverId = (driver.getId() != null) ? driver.getId() : request.getDriverId();
        ride.setDriverId(resolvedDriverId);
        ride.setVehicleId(request.getVehicleId());
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);

        Ride saved = rideRepository.save(ride);
        log.info("Assigned driver '{}' to ride '{}'", resolvedDriverId, rideId);

        // Update driver availability to ON_RIDE (best-effort, non-blocking)
        tryUpdateDriverAvailability(resolvedDriverId, "ON_RIDE");

        return mapToResponse(saved);
    }

    // ================================================================
    // ACCEPT RIDE (by driver)
    // ================================================================

    @Override
    public RideResponse acceptRide(String rideId, String driverAccountId) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.DRIVER_ASSIGNED) {
            throw new InvalidRideStateException(
                "Cannot accept ride in status: " + ride.getStatus() +
                ". Ride must be in DRIVER_ASSIGNED status to accept.");
        }

        // Driver can only accept rides assigned to them
        // Note: driverAccountId is the Account Service ID; ride stores Driver Service driverId.
        // If Account Service ID == Driver Service ID, direct compare. Otherwise adapt.
        validateDriverOwnership(ride, driverAccountId);

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());

        Ride saved = rideRepository.save(ride);
        log.info("Driver '{}' accepted ride '{}'", driverAccountId, rideId);
        return mapToResponse(saved);
    }

    // ================================================================
    // START RIDE (by driver)
    // ================================================================

    @Override
    public RideResponse startRide(String rideId, String driverAccountId) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideStateException(
                "Cannot start ride in status: " + ride.getStatus() +
                ". Ride must be ACCEPTED before starting.");
        }

        validateDriverOwnership(ride, driverAccountId);

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());

        Ride saved = rideRepository.save(ride);
        log.info("Ride '{}' started by driver '{}'", rideId, driverAccountId);
        return mapToResponse(saved);
    }

    // ================================================================
    // COMPLETE RIDE (by driver)
    // ================================================================

    @Override
    public RideResponse completeRide(String rideId, String driverAccountId) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateException(
                "Cannot complete ride in status: " + ride.getStatus() +
                ". Ride must be IN_PROGRESS to complete.");
        }

        validateDriverOwnership(ride, driverAccountId);

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());

        Ride saved = rideRepository.save(ride);
        log.info("Ride '{}' completed by driver '{}'", rideId, driverAccountId);

        // Free up the driver
        if (ride.getDriverId() != null) {
            tryUpdateDriverAvailability(ride.getDriverId(), "AVAILABLE");
        }

        return mapToResponse(saved);
    }

    // ================================================================
    // CANCEL RIDE
    // ================================================================

    @Override
    public RideResponse cancelRide(String rideId, CancelRideRequest request,
                                   String requestingAccountId, String requestingRole) {
        Ride ride = findRideOrThrow(rideId);

        // Enforce cancellable states
        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw new InvalidRideStateException("Cannot cancel a COMPLETED ride.");
        }
        if (ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException("Ride is already CANCELLED.");
        }
        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateException("Cannot cancel a ride that is IN_PROGRESS.");
        }

        // Ownership check: PASSENGER can only cancel their own ride
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requestingRole);
        boolean isPassenger = "PASSENGER".equalsIgnoreCase(requestingRole);
        boolean isDriver = "DRIVER".equalsIgnoreCase(requestingRole);

        if (isPassenger && !ride.getPassengerAccountId().equals(requestingAccountId)) {
            throw new UnauthorizedRideAccessException("You can only cancel your own rides.");
        }

        if (isDriver) {
            // Drivers can cancel (decline) rides assigned to them
            validateDriverOwnership(ride, requestingAccountId);
        }

        String driverIdBeforeCancel = ride.getDriverId();

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancellationReason(request != null ? request.getReason() : null);

        Ride saved = rideRepository.save(ride);
        log.info("Ride '{}' cancelled by '{}' (role: {})", rideId, requestingAccountId, requestingRole);

        // Free up the driver if assigned
        if (driverIdBeforeCancel != null) {
            tryUpdateDriverAvailability(driverIdBeforeCancel, "AVAILABLE");
        }

        return mapToResponse(saved);
    }

    // ================================================================
    // GET PASSENGER RIDES
    // ================================================================

    @Override
    public List<RideResponse> getPassengerRides(String passengerAccountId,
                                                 String requestingAccountId,
                                                 String requestingRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requestingRole);
        if (!isAdmin && !passengerAccountId.equals(requestingAccountId)) {
            throw new UnauthorizedRideAccessException("You can only view your own ride history.");
        }

        return rideRepository.findByPassengerAccountIdOrderByRequestedAtDesc(passengerAccountId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // GET DRIVER RIDES
    // ================================================================

    @Override
    public List<RideResponse> getDriverRides(String driverId,
                                              String requestingAccountId,
                                              String requestingRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requestingRole);
        boolean isDriver = "DRIVER".equalsIgnoreCase(requestingRole);

        if (!isAdmin && isDriver) {
            boolean authorized = driverId.equals(requestingAccountId);
            if (!authorized) {
                try {
                    DriverResponse driver = driverServiceClient.getDriverByAccountId(requestingAccountId);
                    if (driver != null && (driverId.equals(driver.getId()) || driverId.equals(driver.getAccountId()))) {
                        authorized = true;
                    }
                } catch (Exception e) {
                    log.warn("Could not verify driver ownership for driverId '{}': {}", driverId, e.getMessage());
                }
            }

            if (!authorized) {
                throw new UnauthorizedRideAccessException("You can only view your own ride history.");
            }
        }

        List<Ride> rides = rideRepository.findByDriverIdOrderByRequestedAtDesc(driverId);
        if (rides.isEmpty()) {
            try {
                DriverResponse driver = driverServiceClient.getDriverByAccountId(driverId);
                if (driver != null && driver.getId() != null && !driver.getId().equals(driverId)) {
                    rides = rideRepository.findByDriverIdOrderByRequestedAtDesc(driver.getId());
                }
            } catch (Exception e) {
                log.debug("Fallback query for driver rides by alternate ID skipped: {}", e.getMessage());
            }
        }

        return rides.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // GET RIDES BY STATUS
    // ================================================================

    @Override
    public List<RideResponse> getRidesByStatus(RideStatus status) {
        return rideRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private Ride findRideOrThrow(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with ID: " + rideId));
    }

    /**
     * Validate that the requesting driver is the one assigned to the ride.
     * driverAccountId here is the account ID from JWT.
     * ride.getDriverId() is the Driver Service entity ID.
     *
     * IMPORTANT: If Account Service ID != Driver Service entity ID, you need to
     * look up the driver via DriverServiceClient by accountId to get the driverId.
     * Adapt this method when integrating with actual Member 2 API.
     */
    private void validateDriverOwnership(Ride ride, String driverAccountId) {
        if (ride.getDriverId() == null) {
            throw new UnauthorizedRideAccessException("No driver is assigned to this ride.");
        }
        // Direct compare - works when driverId stored in ride == JWT account ID
        if (ride.getDriverId().equals(driverAccountId)) {
            return;
        }
        // If they differ, resolve via Driver Service by driverAccountId
        try {
            DriverResponse driver = driverServiceClient.getDriverByAccountId(driverAccountId);
            if (driver != null && ride.getDriverId().equals(driver.getId())) {
                return;
            }
        } catch (Exception e) {
            log.warn("Could not verify driver ownership via Driver Service: {}", e.getMessage());
        }
        throw new UnauthorizedRideAccessException("This ride is not assigned to you.");
    }

    /**
     * Best-effort attempt to update driver availability via Driver Service.
     * Failures are logged but do not cause the ride operation to fail.
     */
    private void tryUpdateDriverAvailability(String driverId, String status) {
        try {
            driverServiceClient.updateDriverAvailability(driverId, status);
            log.debug("Updated driver '{}' availability to '{}'", driverId, status);
        } catch (Exception e) {
            log.warn("Failed to update driver '{}' availability to '{}': {}",
                    driverId, status, e.getMessage());
        }
    }

    // ================================================================
    // MAPPING HELPERS
    // ================================================================

    private Location mapToLocation(LocationRequest req) {
        Location loc = new Location();
        loc.setAddress(req.getAddress());
        loc.setLatitude(req.getLatitude());
        loc.setLongitude(req.getLongitude());
        return loc;
    }

    private LocationResponse mapToLocationResponse(Location loc) {
        if (loc == null) return null;
        return new LocationResponse(loc.getAddress(), loc.getLatitude(), loc.getLongitude());
    }

    public RideResponse mapToResponse(Ride ride) {
        RideResponse res = new RideResponse();
        res.setId(ride.getId());
        res.setPassengerAccountId(ride.getPassengerAccountId());
        res.setDriverId(ride.getDriverId());
        res.setVehicleId(ride.getVehicleId());
        res.setPickupLocation(mapToLocationResponse(ride.getPickupLocation()));
        res.setDestinationLocation(mapToLocationResponse(ride.getDestinationLocation()));
        res.setStatus(ride.getStatus());
        res.setRequestedAt(ride.getRequestedAt());
        res.setAcceptedAt(ride.getAcceptedAt());
        res.setStartedAt(ride.getStartedAt());
        res.setCompletedAt(ride.getCompletedAt());
        res.setCancelledAt(ride.getCancelledAt());
        res.setCancellationReason(ride.getCancellationReason());
        return res;
    }
}
