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
import org.mockito.ArgumentCaptor;
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
    // 1. Create ride successfully (auto-assigns longest-available driver)
    // ================================================================

    @Test
    @DisplayName("1. Create ride successfully and auto-assign a driver")
    void createRide_shouldReturnRideResponse_whenValidRequest() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        DriverResponse driver = eligibleDriver(DRIVER_ID, VEHICLE_ID, LocalDateTime.now().minusMinutes(30));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of(driver));
        when(driverServiceClient.reserveDriver(DRIVER_ID, null))
                .thenReturn(new DriverReservationResponse(true, DRIVER_ID, VEHICLE_ID, null));

        RideResponse response = rideService.createRide(createRideRequest, PASSENGER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPassengerAccountId()).isEqualTo(PASSENGER_ID);
        assertThat(response.getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(response.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
        // The atomic reservation itself flips the driver ON_RIDE; no separate availability call.
        verify(driverServiceClient).reserveDriver(DRIVER_ID, null);
        verify(driverServiceClient, never()).updateDriverAvailability(DRIVER_ID, "ON_RIDE");
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
    // 16. Longest-available driver is selected first (FIFO)
    // ================================================================

    @Test
    @DisplayName("16. Longest-available eligible driver is selected")
    void createRide_shouldSelectLongestAvailableDriver() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        // Driver Service returns eligible drivers already ordered by availableSince ASC.
        DriverResponse driverA = eligibleDriver("driver-A", "vehicle-A", LocalDateTime.now().minusHours(2));
        DriverResponse driverB = eligibleDriver("driver-B", "vehicle-B", LocalDateTime.now().minusHours(1));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of(driverA, driverB));
        when(driverServiceClient.reserveDriver("driver-A", null))
                .thenReturn(new DriverReservationResponse(true, "driver-A", "vehicle-A", null));

        RideResponse response = rideService.createRide(createRideRequest, PASSENGER_ID);

        assertThat(response.getDriverId()).isEqualTo("driver-A");
        assertThat(response.getVehicleId()).isEqualTo("vehicle-A");
        assertThat(response.getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        // The longest-available driver was reserved first; the second was never attempted.
        verify(driverServiceClient, never()).reserveDriver(eq("driver-B"), any());
    }

    // ================================================================
    // 17. A driver whose reservation fails (no ACTIVE vehicle) is skipped; next eligible wins
    // ================================================================

    @Test
    @DisplayName("17. Driver whose reservation fails is skipped, next eligible selected")
    void createRide_shouldSkipDriverWhoseReservationFails() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        DriverResponse driverA = eligibleDriver("driver-A", "vehicle-A", LocalDateTime.now().minusHours(3));
        DriverResponse driverB = eligibleDriver("driver-B", "vehicle-B", LocalDateTime.now().minusHours(2));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of(driverA, driverB));
        // driver-A's vehicle vanished between listing and reservation -> reservation fails.
        when(driverServiceClient.reserveDriver("driver-A", null))
                .thenReturn(new DriverReservationResponse(false, "driver-A", null, "No ACTIVE vehicle for driver"));
        when(driverServiceClient.reserveDriver("driver-B", null))
                .thenReturn(new DriverReservationResponse(true, "driver-B", "vehicle-B", null));

        RideResponse response = rideService.createRide(createRideRequest, PASSENGER_ID);

        assertThat(response.getDriverId()).isEqualTo("driver-B");
        assertThat(response.getVehicleId()).isEqualTo("vehicle-B");
    }

    // ================================================================
    // 18. No available driver keeps ride REQUESTED and raises the existing error
    // ================================================================

    @Test
    @DisplayName("18. No available driver keeps ride REQUESTED and throws DriverNotAvailableException")
    void createRide_shouldKeepRequestedAndThrow_whenNoEligibleDrivers() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.createRide(createRideRequest, PASSENGER_ID))
                .isInstanceOf(DriverNotAvailableException.class)
                .hasMessageContaining("No available drivers");

        ArgumentCaptor<Ride> captor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(captor.getValue().getDriverId()).isNull();
        verify(driverServiceClient, never()).reserveDriver(any(), any());
    }

    // ================================================================
    // 19. Same driver cannot be assigned to two active rides (concurrency guard)
    // ================================================================

    @Test
    @DisplayName("19. Driver already on an active ride is skipped")
    void createRide_shouldSkipDriverWithActiveRide() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        DriverResponse driverA = eligibleDriver("driver-A", "vehicle-A", LocalDateTime.now().minusHours(2));
        DriverResponse driverB = eligibleDriver("driver-B", "vehicle-B", LocalDateTime.now().minusHours(1));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of(driverA, driverB));

        Ride activeRide = buildRide("ride-active", PASSENGER_ID, RideStatus.IN_PROGRESS);
        activeRide.setDriverId("driver-A");
        when(rideRepository.findByDriverId("driver-A")).thenReturn(List.of(activeRide));
        when(driverServiceClient.reserveDriver("driver-B", null))
                .thenReturn(new DriverReservationResponse(true, "driver-B", "vehicle-B", null));

        RideResponse response = rideService.createRide(createRideRequest, PASSENGER_ID);

        assertThat(response.getDriverId()).isEqualTo("driver-B");
        assertThat(response.getVehicleId()).isEqualTo("vehicle-B");
        // driver-A was pre-filtered by the active-ride guard, so it was never reserved.
        verify(driverServiceClient, never()).reserveDriver(eq("driver-A"), any());
    }

    // ================================================================
    // 20. Driver becomes AVAILABLE after ride completion
    // ================================================================

    @Test
    @DisplayName("20. Driver is freed (AVAILABLE) after ride completion")
    void completeRide_shouldFreeDriver() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.IN_PROGRESS);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.completeRide(RIDE_ID, DRIVER_ID);

        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
        verify(driverServiceClient).updateDriverAvailability(DRIVER_ID, "AVAILABLE");
    }

    // ================================================================
    // 21. Driver is freed (AVAILABLE) after ride cancellation
    // ================================================================

    @Test
    @DisplayName("21. Assigned driver is freed (AVAILABLE) after cancellation")
    void cancelRide_shouldFreeAssignedDriver() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.DRIVER_ASSIGNED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide(
                RIDE_ID, new CancelRideRequest("Driver declined"), DRIVER_ID, "DRIVER");

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        verify(driverServiceClient).updateDriverAvailability(DRIVER_ID, "AVAILABLE");
    }

    // ================================================================
    // 22. Pickup service area is used to filter eligible drivers
    // ================================================================

    @Test
    @DisplayName("22. Pickup service area filters eligible drivers before selection")
    void createRide_shouldFilterByPickupServiceArea() {
        LocationRequest pickup = new LocationRequest("Pickup St", "Divulapitiya", 6.9271, 79.8612);
        CreateRideRequest request = new CreateRideRequest(pickup, sampleDestination);

        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        // Driver Service already applied service-area filtering + longest-available ordering.
        DriverResponse driverB = eligibleDriver("driver-B", "vehicle-B", LocalDateTime.now().minusMinutes(90));
        when(driverServiceClient.getEligibleDrivers("Divulapitiya", null)).thenReturn(List.of(driverB));
        when(driverServiceClient.reserveDriver("driver-B", null))
                .thenReturn(new DriverReservationResponse(true, "driver-B", "vehicle-B", null));

        RideResponse response = rideService.createRide(request, PASSENGER_ID);

        assertThat(response.getDriverId()).isEqualTo("driver-B");
        verify(driverServiceClient).getEligibleDrivers("Divulapitiya", null);
    }

    // ================================================================
    // 23. Every reservation fails -> ride stays REQUESTED, existing no-driver error
    // ================================================================

    @Test
    @DisplayName("23. Ride stays REQUESTED when all reservations fail")
    void createRide_shouldKeepRequested_whenAllReservationsFail() {
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        DriverResponse driverA = eligibleDriver("driver-A", "vehicle-A", LocalDateTime.now().minusHours(2));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of(driverA));
        when(driverServiceClient.reserveDriver("driver-A", null))
                .thenReturn(new DriverReservationResponse(false, "driver-A", null, "already reserved"));

        assertThatThrownBy(() -> rideService.createRide(createRideRequest, PASSENGER_ID))
                .isInstanceOf(DriverNotAvailableException.class)
                .hasMessageContaining("No available drivers");

        ArgumentCaptor<Ride> captor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RideStatus.REQUESTED);
    }

    // ================================================================
    // 24. Rollback: driver released if assignment persistence fails after reservation
    // ================================================================

    @Test
    @DisplayName("24. Driver is released to AVAILABLE if assignment save fails after reservation")
    void createRide_shouldReleaseDriver_whenAssignmentSaveFails() {
        // First save persists REQUESTED; the second (assignment) save fails.
        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("db down"));
        DriverResponse driverA = eligibleDriver("driver-A", "vehicle-A", LocalDateTime.now().minusHours(1));
        when(driverServiceClient.getEligibleDrivers(null, null)).thenReturn(List.of(driverA));
        when(driverServiceClient.reserveDriver("driver-A", null))
                .thenReturn(new DriverReservationResponse(true, "driver-A", "vehicle-A", null));

        assertThatThrownBy(() -> rideService.createRide(createRideRequest, PASSENGER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        verify(driverServiceClient).updateDriverAvailability("driver-A", "AVAILABLE");
    }

    // ================================================================
    // 25. Admin can list all rides
    // ================================================================

    @Test
    @DisplayName("25. Admin can list all rides without filter")
    void getAllRides_shouldReturnAllRides_whenNoStatusFilter() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.REQUESTED);
        Ride ride2 = buildRide("ride-2", "passenger-2", RideStatus.COMPLETED);
        ride2.setDriverId(DRIVER_ID);
        when(rideRepository.findAll()).thenReturn(List.of(ride1, ride2));

        List<RideResponse> rides = rideService.getAllRides(null);

        assertThat(rides).hasSize(2);
        verify(rideRepository).findAll();
    }

    // ================================================================
    // 26. Admin can filter rides by status
    // ================================================================

    @Test
    @DisplayName("26. Admin can filter rides by status")
    void getAllRides_shouldFilterByStatus() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.IN_PROGRESS);
        ride1.setDriverId(DRIVER_ID);
        when(rideRepository.findByStatus(RideStatus.IN_PROGRESS)).thenReturn(List.of(ride1));

        List<RideResponse> rides = rideService.getAllRides(RideStatus.IN_PROGRESS);

        assertThat(rides).hasSize(1);
        assertThat(rides.get(0).getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        verify(rideRepository).findByStatus(RideStatus.IN_PROGRESS);
    }

    // ================================================================
    // 27. Admin can monitor REQUESTED rides
    // ================================================================

    @Test
    @DisplayName("27. Admin can monitor rides by REQUESTED status")
    void getRidesByStatus_shouldReturnRequestedRides() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.REQUESTED);
        Ride ride2 = buildRide("ride-2", "passenger-2", RideStatus.REQUESTED);
        when(rideRepository.findByStatus(RideStatus.REQUESTED)).thenReturn(List.of(ride1, ride2));

        List<RideResponse> rides = rideService.getRidesByStatus(RideStatus.REQUESTED);

        assertThat(rides).hasSize(2);
        assertThat(rides).allMatch(r -> r.getStatus() == RideStatus.REQUESTED);
    }

    // ================================================================
    // 28. Admin can monitor COMPLETED rides
    // ================================================================

    @Test
    @DisplayName("28. Admin can monitor rides by COMPLETED status")
    void getRidesByStatus_shouldReturnCompletedRides() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.COMPLETED);
        ride1.setDriverId(DRIVER_ID);
        ride1.setCompletedAt(LocalDateTime.now());
        when(rideRepository.findByStatus(RideStatus.COMPLETED)).thenReturn(List.of(ride1));

        List<RideResponse> rides = rideService.getRidesByStatus(RideStatus.COMPLETED);

        assertThat(rides).hasSize(1);
        assertThat(rides.get(0).getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    // ================================================================
    // 29. Admin can view ride details including passenger/driver/vehicle assignment
    // ================================================================

    @Test
    @DisplayName("29. Admin can view ride with driver and vehicle assignment info")
    void getRideById_shouldReturnFullRideWithAssignment() {
        Ride ride = buildRide(RIDE_ID, PASSENGER_ID, RideStatus.DRIVER_ASSIGNED);
        ride.setDriverId(DRIVER_ID);
        ride.setVehicleId(VEHICLE_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        RideResponse response = rideService.getRideById(RIDE_ID);

        assertThat(response.getPassengerAccountId()).isEqualTo(PASSENGER_ID);
        assertThat(response.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(response.getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
    }

    // ================================================================
    // 30. Admin can view passenger ride history
    // ================================================================

    @Test
    @DisplayName("30. Admin can view any passenger's ride history")
    void getPassengerRides_shouldReturnRides_whenAdminRequests() {
        Ride ride1 = buildRide("ride-1", PASSENGER_ID, RideStatus.COMPLETED);
        Ride ride2 = buildRide("ride-2", PASSENGER_ID, RideStatus.IN_PROGRESS);
        ride2.setDriverId(DRIVER_ID);
        when(rideRepository.findByPassengerAccountIdOrderByRequestedAtDesc(PASSENGER_ID))
                .thenReturn(List.of(ride1, ride2));

        List<RideResponse> rides = rideService.getPassengerRides(PASSENGER_ID, "admin-id", "ADMIN");

        assertThat(rides).hasSize(2);
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private DriverResponse eligibleDriver(String id, String activeVehicleId, LocalDateTime availableSince) {
        DriverResponse driver = new DriverResponse();
        driver.setId(id);
        driver.setAvailabilityStatus("AVAILABLE");
        driver.setActiveVehicleId(activeVehicleId);
        driver.setAvailableSince(availableSince);
        return driver;
    }

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
