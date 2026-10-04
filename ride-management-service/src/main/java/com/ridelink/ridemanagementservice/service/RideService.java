package com.ridelink.ridemanagementservice.service;

import com.ridelink.ridemanagementservice.dto.AssignDriverRequest;
import com.ridelink.ridemanagementservice.dto.CancelRideRequest;
import com.ridelink.ridemanagementservice.dto.CreateRideRequest;
import com.ridelink.ridemanagementservice.dto.RideResponse;
import com.ridelink.ridemanagementservice.model.RideStatus;

import java.util.List;

/**
 * Service interface for ride management business logic.
 */
public interface RideService {

    RideResponse createRide(CreateRideRequest request, String passengerAccountId);

    RideResponse getRideById(String rideId);

    RideResponse assignDriver(String rideId, AssignDriverRequest request, String requestingAccountId);

    RideResponse acceptRide(String rideId, String driverAccountId);

    RideResponse startRide(String rideId, String driverAccountId);

    RideResponse completeRide(String rideId, String driverAccountId);

    RideResponse cancelRide(String rideId, CancelRideRequest request, String requestingAccountId, String requestingRole);

    List<RideResponse> getPassengerRides(String passengerAccountId, String requestingAccountId, String requestingRole);

    List<RideResponse> getDriverRides(String driverId, String requestingAccountId, String requestingRole);

    List<RideResponse> getRidesByStatus(RideStatus status);
}
