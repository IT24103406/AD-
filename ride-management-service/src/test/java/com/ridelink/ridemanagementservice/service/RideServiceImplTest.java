package com.ridelink.ridemanagementservice.service;

import com.ridelink.ridemanagementservice.dto.*;
import com.ridelink.ridemanagementservice.exception.*;
import com.ridelink.ridemanagementservice.model.Location;
import com.ridelink.ridemanagementservice.model.Ride;
import com.ridelink.ridemanagementservice.model.RideStatus;
import com.ridelink.ridemanagementservice.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RideServiceImpl.
 *
 * No real MongoDB or Driver Service is used.
 * RideRepository and DriverServiceClient are mocked.
 */
@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverServiceClient driverServiceClient;

    @InjectMocks
    private RideServiceImpl rideService;

    // ---- Test data ----

    private static final String PASSENGER_ID = "passenger-123";
    private static final String DRIVER_ID = "driver-456";
    private static final String VEHICLE_ID = "vehicle-789";
    private static final String RIDE_ID = "ride-001";

    private LocationRequest samplePickup;
    private LocationRequest sampleDestination;
    private CreateRideRequest createRideRequest;

    @BeforeEach
    void setUp() {
        samplePickup = new LocationRequest("123 Main St", 6.9271, 79.8612);
        sampleDestination = new LocationRequest("456 Park Ave", 6.9147, 79.8525);
        createRideRequest = new CreateRideRequest(samplePickup, sampleDestination);
    }

    // ================================================================
    // 1. Create ride successfully
    // ================================================================

    @Test
    @DisplayName("1. Create ride successfully")
    void createRide_shouldReturnRideResponse_whenValidRequest() {
        Ride savedRide = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.REQUESTED);
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);

        RideResponse response = rideService.createRide(createRideRequest, PASSENGER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPassengerAccountId()).isEqualTo(PASSENGER_ID);
        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    // ================================================================
    // 2. Retrieve ride successfully
    // ================================================================

    @Test
    @DisplayName("2. Retrieve ride successfully")
    void getRideById_shouldReturnRideResponse_whenRideExists() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.REQUESTED);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        RideResponse response = rideService.getRideById(RIDE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(RIDE_ID);
    }

    // ================================================================
    // 3. Ride not found
    // ================================================================

    @Test
    @DisplayName("3. Ride not found throws RideNotFoundException")
    void getRideById_shouldThrowRideNotFoundException_whenRideDoesNotExist() {
        when(rideRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getRideById("nonexistent"))
                .isInstanceOf(RideNotFoundException.class)
                .hasMessageContaining("Ride not found");
    }

    // ================================================================
    // 4. Assign driver successfully
    // ================================================================

    @Test
    @DisplayName("4. Assign driver successfully")
    void assignDriver_shouldAssignDriver_whenRideIsRequestedAndDriverAvailable() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.REQUESTED);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        DriverResponse driverResponse = new DriverResponse();
        driverResponse.setId(DRIVER_ID);
        driverResponse.setAvailabilityStatus("AVAILABLE");
        when(driverServiceClient.getDriverById(DRIVER_ID)).thenReturn(driverResponse);

        Ride savedRide = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.DRIVER_ASSIGNED);
        savedRide.setDriverId(DRIVER_ID);
        savedRide.setVehicleId(VEHICLE_ID);
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);

        AssignDriverRequest request = new AssignDriverRequest(DRIVER_ID, VEHICLE_ID);
        RideResponse response = rideService.assignDriver(RIDE_ID, request, "admin-id");

        assertThat(response.getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(response.getDriverId()).isEqualTo(DRIVER_ID);
    }

    // ================================================================
    // 5. Cannot accept a REQUESTED ride before driver assignment
    // ================================================================

    @Test
    @DisplayName("5. Cannot accept a REQUESTED ride before driver assignment")
    void acceptRide_shouldThrowInvalidRideStateException_whenRideIsRequested() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.REQUESTED);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide(RIDE_ID, DRIVER_ID))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("DRIVER_ASSIGNED");
    }

    // ================================================================
    // 6. Accept assigned ride successfully
    // ================================================================

    @Test
    @DisplayName("6. Accept assigned ride successfully")
    void acceptRide_shouldAcceptRide_whenRideIsDriverAssigned() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.DRIVER_ASSIGNED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        Ride savedRide = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.ACCEPTED);
        savedRide.setDriverId(DRIVER_ID);
        savedRide.setAcceptedAt(LocalDateTime.now());
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);

        RideResponse response = rideService.acceptRide(RIDE_ID, DRIVER_ID);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
    }

    // ================================================================
    // 7. Start accepted ride successfully
    // ================================================================

    @Test
    @DisplayName("7. Start accepted ride successfully")
    void startRide_shouldStartRide_whenRideIsAccepted() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.ACCEPTED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        Ride savedRide = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.IN_PROGRESS);
        savedRide.setDriverId(DRIVER_ID);
        savedRide.setStartedAt(LocalDateTime.now());
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);

        RideResponse response = rideService.startRide(RIDE_ID, DRIVER_ID);

        assertThat(response.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    // ================================================================
    // 8. Cannot start ride in invalid state
    // ================================================================

    @Test
    @DisplayName("8. Cannot start ride in invalid state (REQUESTED)")
    void startRide_shouldThrowInvalidRideStateException_whenRideNotAccepted() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.REQUESTED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.startRide(RIDE_ID, DRIVER_ID))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("ACCEPTED");
    }

    // ================================================================
    // 9. Complete in-progress ride successfully
    // ================================================================

    @Test
    @DisplayName("9. Complete in-progress ride successfully")
    void completeRide_shouldCompleteRide_whenRideIsInProgress() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.IN_PROGRESS);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        Ride savedRide = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.COMPLETED);
        savedRide.setDriverId(DRIVER_ID);
        savedRide.setCompletedAt(LocalDateTime.now());
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);

        RideResponse response = rideService.completeRide(RIDE_ID, DRIVER_ID);

        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    // ================================================================
    // 10. Cannot complete invalid ride state
    // ================================================================

    @Test
    @DisplayName("10. Cannot complete ride not in IN_PROGRESS state")
    void completeRide_shouldThrowInvalidRideStateException_whenRideNotInProgress() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.ACCEPTED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.completeRide(RIDE_ID, DRIVER_ID))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    // ================================================================
    // 11. Cancel valid ride successfully
    // ================================================================

    @Test
    @DisplayName("11. Cancel valid ride successfully (PASSENGER cancels own REQUESTED ride)")
    void cancelRide_shouldCancelRide_whenPassengerCancelsOwnRequestedRide() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.REQUESTED);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        Ride savedRide = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.CANCELLED);
        savedRide.setCancellationReason("Changed my mind");
        savedRide.setCancelledAt(LocalDateTime.now());
        when(rideRepository.save(any(Ride.class))).thenReturn(savedRide);

        CancelRideRequest cancelRequest = new CancelRideRequest("Changed my mind");
        RideResponse response = rideService.cancelRide(RIDE_ID, cancelRequest, PASSENGER_ID, "PASSENGER");

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
    }

    // ================================================================
    // 12. Cannot cancel completed ride
    // ================================================================

    @Test
    @DisplayName("12. Cannot cancel a COMPLETED ride")
    void cancelRide_shouldThrowInvalidRideStateException_whenRideIsCompleted() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.COMPLETED);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        CancelRideRequest cancelRequest = new CancelRideRequest("Trying to cancel");
        assertThatThrownBy(() -> rideService.cancelRide(RIDE_ID, cancelRequest, PASSENGER_ID, "PASSENGER"))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    // ================================================================
    // 13. Passenger ride history
    // ================================================================

    @Test
    @DisplayName("13. Get passenger ride history")
    void getPassengerRides_shouldReturnRideList_whenPassengerViewsOwnRides() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.COMPLETED);
        Ride ride2 = buildRide("ride-2", PASSENGER_ID, RideStatus.CANCELLED);
        when(rideRepository.findByPassengerAccountIdOrderByRequestedAtDesc(PASSENGER_ID))
                .thenReturn(List.of(ride1, ride2));

        List<RideResponse> rides = rideService.getPassengerRides(PASSENGER_ID, PASSENGER_ID, "PASSENGER");

        assertThat(rides).hasSize(2);
        assertThat(rides).allMatch(r -> r.getPassengerAccountId().equals(PASSENGER_ID));
    }

    // ================================================================
    // 14. Driver ride history
    // ================================================================

    @Test
    @DisplayName("14. Get driver ride history")
    void getDriverRides_shouldReturnRideList_whenDriverViewsOwnRides() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.COMPLETED);
        ride1.setDriverId(DRIVER_ID);
        when(rideRepository.findByDriverIdOrderByRequestedAtDesc(DRIVER_ID))
                .thenReturn(List.of(ride1));

        List<RideResponse> rides = rideService.getDriverRides(DRIVER_ID, DRIVER_ID, "DRIVER");

        assertThat(rides).hasSize(1);
    }

    // ================================================================
    // 15. Unauthorized ownership scenario
    // ================================================================

    @Test
    @DisplayName("15. Passenger cannot cancel another passenger's ride")
    void cancelRide_shouldThrowUnauthorizedRideAccessException_whenPassengerCancelsOtherRide() {
        Ride ride = buildRide(RIDE_ID, "other-passenger-id", RideStatus.REQUESTED);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        CancelRideRequest cancelRequest = new CancelRideRequest("Unauthorized attempt");
        assertThatThrownBy(() -> rideService.cancelRide(RIDE_ID, cancelRequest, PASSENGER_ID, "PASSENGER"))
                .isInstanceOf(UnauthorizedRideAccessException.class)
                .hasMessageContaining("own rides");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Ride buildRide(String id, String passengerId, RideStatus status) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerAccountId(passengerId);
        ride.setStatus(status);
        ride.setRequestedAt(LocalDateTime.now());
        ride.setPickupLocation(new Location("123 Main St", 6.9271, 79.8612));
        ride.setDestinationLocation(new Location("456 Park Ave", 6.9147, 79.8525));
        return ride;
    }
}
