package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.client.AccountServiceClient;
import com.ridelink.drivervehicleservice.dto.request.AvailabilityRequest;
import com.ridelink.drivervehicleservice.dto.request.DriverRequest;
import com.ridelink.drivervehicleservice.dto.response.AccountUserResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverReservationResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverResponse;
import com.ridelink.drivervehicleservice.exception.DuplicateResourceException;
import com.ridelink.drivervehicleservice.exception.InvalidAccountException;
import com.ridelink.drivervehicleservice.exception.ResourceNotFoundException;
import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.Vehicle;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleStatus;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import com.ridelink.drivervehicleservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private DriverService driverService;

    private Driver driver;
    private DriverRequest request;
    private AccountUserResponse accountUser;

    @BeforeEach
    void setUp() {
        driver = new Driver();
        driver.setId("1");
        driver.setAccountId("acc1");
        driver.setLicenseNumber("LIC123");

        request = new DriverRequest("acc1", "John", "Doe", "1234567890", "LIC123", LocalDate.now().plusYears(1));

        accountUser = new AccountUserResponse();
        accountUser.setId("acc1");
        accountUser.setRole("DRIVER");
        accountUser.setStatus("ACTIVE");
    }

    @Test
    void createDriver_Success() {
        when(driverRepository.existsByAccountId(any())).thenReturn(false);
        when(driverRepository.existsByLicenseNumber(any())).thenReturn(false);
        when(accountServiceClient.getUserById("acc1")).thenReturn(accountUser);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        DriverResponse response = driverService.createDriver(request);

        assertNotNull(response);
        assertEquals("acc1", response.accountId());
        verify(accountServiceClient, times(1)).getUserById("acc1");
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void createDriver_DuplicateAccount() {
        when(driverRepository.existsByAccountId(any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> driverService.createDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
        verify(accountServiceClient, never()).getUserById(any());
    }

    @Test
    void createDriver_InvalidRole() {
        accountUser.setRole("PASSENGER");
        when(driverRepository.existsByAccountId(any())).thenReturn(false);
        when(driverRepository.existsByLicenseNumber(any())).thenReturn(false);
        when(accountServiceClient.getUserById("acc1")).thenReturn(accountUser);

        assertThrows(InvalidAccountException.class, () -> driverService.createDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void updateAvailability_Success() {
        when(driverRepository.findById("1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        DriverResponse response = driverService.updateAvailability("1", new AvailabilityRequest(AvailabilityStatus.AVAILABLE));

        assertNotNull(response);
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void updateAvailability_NotFound() {
        when(driverRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                driverService.updateAvailability("1", new AvailabilityRequest(AvailabilityStatus.AVAILABLE)));
    }

    @Test
    void updateAvailability_setsAvailableSinceWhenBecomesAvailable() {
        driver.setAvailabilityStatus(AvailabilityStatus.ON_RIDE);
        driver.setAvailableSince(null);
        when(driverRepository.findById("1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        driverService.updateAvailability("1", new AvailabilityRequest(AvailabilityStatus.AVAILABLE));

        assertEquals(AvailabilityStatus.AVAILABLE, driver.getAvailabilityStatus());
        assertNotNull(driver.getAvailableSince());
    }

    @Test
    void updateAvailability_doesNotSetAvailableSinceWhenNotAvailable() {
        driver.setAvailableSince(null);
        when(driverRepository.findById("1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        driverService.updateAvailability("1", new AvailabilityRequest(AvailabilityStatus.ON_RIDE));

        assertEquals(AvailabilityStatus.ON_RIDE, driver.getAvailabilityStatus());
        assertNull(driver.getAvailableSince());
    }

    @Test
    void getEligibleDrivers_ordersLongestAvailableFirst() {
        Driver a = availableDriver("dA", LocalDateTime.now().minusHours(1));
        Driver b = availableDriver("dB", LocalDateTime.now().minusHours(3)); // free the longest
        when(driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE))
                .thenReturn(new ArrayList<>(List.of(a, b)));
        when(vehicleRepository.findByDriverIdAndVehicleStatus("dA", VehicleStatus.ACTIVE))
                .thenReturn(List.of(activeVehicle("vA", "dA")));
        when(vehicleRepository.findByDriverIdAndVehicleStatus("dB", VehicleStatus.ACTIVE))
                .thenReturn(List.of(activeVehicle("vB", "dB")));

        List<DriverResponse> result = driverService.getEligibleDrivers(null, null);

        assertEquals(2, result.size());
        assertEquals("dB", result.get(0).id());
        assertEquals("vB", result.get(0).activeVehicleId());
        assertEquals("dA", result.get(1).id());
    }

    @Test
    void getEligibleDrivers_skipsDriverWithoutActiveVehicle() {
        Driver a = availableDriver("dA", LocalDateTime.now().minusHours(3));
        Driver b = availableDriver("dB", LocalDateTime.now().minusHours(1));
        when(driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE))
                .thenReturn(new ArrayList<>(List.of(a, b)));
        when(vehicleRepository.findByDriverIdAndVehicleStatus("dA", VehicleStatus.ACTIVE))
                .thenReturn(List.of()); // no ACTIVE vehicle -> skipped
        when(vehicleRepository.findByDriverIdAndVehicleStatus("dB", VehicleStatus.ACTIVE))
                .thenReturn(List.of(activeVehicle("vB", "dB")));

        List<DriverResponse> result = driverService.getEligibleDrivers(null, null);

        assertEquals(1, result.size());
        assertEquals("dB", result.get(0).id());
        assertEquals("vB", result.get(0).activeVehicleId());
    }

    @Test
    void getEligibleDrivers_onlyQueriesAvailableDrivers() {
        when(driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE))
                .thenReturn(new ArrayList<>());

        List<DriverResponse> result = driverService.getEligibleDrivers(null, null);

        assertTrue(result.isEmpty());
        verify(driverRepository).findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        verify(driverRepository, never()).findByAvailabilityStatus(AvailabilityStatus.ON_RIDE);
        verify(driverRepository, never()).findByAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
    }

    @Test
    void getEligibleDrivers_filtersByServiceAreaBeforeSorting() {
        // A globally older driver OUTSIDE the area (dA) and a younger driver INSIDE it (dB).
        Driver outOfArea = availableDriver("dA", LocalDateTime.now().minusHours(5));
        Driver inArea = availableDriver("dB", LocalDateTime.now().minusHours(1));
        // The area query returns only the in-area AVAILABLE driver.
        when(driverRepository.findEligibleDriversByServiceArea(AvailabilityStatus.AVAILABLE, "Divulapitiya"))
                .thenReturn(new ArrayList<>(List.of(inArea)));
        when(vehicleRepository.findByDriverIdAndVehicleStatus("dB", VehicleStatus.ACTIVE))
                .thenReturn(List.of(activeVehicle("vB", "dB")));

        List<DriverResponse> result = driverService.getEligibleDrivers("Divulapitiya", null);

        assertEquals(1, result.size());
        assertEquals("dB", result.get(0).id());
        // Service-area path must not fall back to the unfiltered "all AVAILABLE" query.
        verify(driverRepository).findEligibleDriversByServiceArea(AvailabilityStatus.AVAILABLE, "Divulapitiya");
        verify(driverRepository, never()).findByAvailabilityStatus(any());
        // outOfArea is never considered even though it is globally older.
        assertTrue(result.stream().noneMatch(d -> d.id().equals("dA")));
        assertNotNull(outOfArea.getAvailableSince());
    }

    @Test
    void reserveDriver_success_whenAvailableAndHasActiveVehicle() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Driver.class))).thenReturn(driver);
        when(vehicleRepository.findByDriverIdAndVehicleStatus("1", VehicleStatus.ACTIVE))
                .thenReturn(List.of(activeVehicle("v1", "1")));

        DriverReservationResponse result = driverService.reserveDriver("1", null);

        assertTrue(result.reserved());
        assertEquals("1", result.driverId());
        assertEquals("v1", result.vehicleId());
        // Successful reservation must NOT release the driver.
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Driver.class));
    }

    @Test
    void reserveDriver_fails_whenAtomicUpdateMatchesNothing() {
        // Another concurrent request already flipped the driver out of AVAILABLE.
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Driver.class))).thenReturn(null);

        DriverReservationResponse result = driverService.reserveDriver("1", null);

        assertFalse(result.reserved());
        assertNull(result.vehicleId());
        // No vehicle lookup and no release when the driver was never reserved.
        verify(vehicleRepository, never()).findByDriverIdAndVehicleStatus(any(), any());
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Driver.class));
    }

    @Test
    void reserveDriver_releasesDriver_whenNoActiveVehicle() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Driver.class))).thenReturn(driver);
        when(vehicleRepository.findByDriverIdAndVehicleStatus("1", VehicleStatus.ACTIVE))
                .thenReturn(List.of());

        DriverReservationResponse result = driverService.reserveDriver("1", null);

        assertFalse(result.reserved());
        // Driver must be released back to AVAILABLE so it is not stuck ON_RIDE.
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Driver.class));
    }

    private Driver availableDriver(String id, LocalDateTime availableSince) {
        Driver d = new Driver();
        d.setId(id);
        d.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        d.setAvailableSince(availableSince);
        return d;
    }

    private Vehicle activeVehicle(String id, String driverId) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setDriverId(driverId);
        v.setVehicleStatus(VehicleStatus.ACTIVE);
        return v;
    }
}
