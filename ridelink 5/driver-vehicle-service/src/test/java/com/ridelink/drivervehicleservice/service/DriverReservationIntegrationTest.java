package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.dto.response.DriverReservationResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverResponse;
import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.ServiceArea;
import com.ridelink.drivervehicleservice.model.Vehicle;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleType;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import com.ridelink.drivervehicleservice.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test against a real (local) MongoDB test database.
 *
 * Verifies the behaviours that unit tests with mocks cannot truly prove:
 * - the atomic AVAILABLE -> ON_RIDE reservation lets exactly one of two concurrent callers win;
 * - service-area filtering excludes a globally-older out-of-area driver.
 *
 * No external production services are required; only MongoDB on localhost.
 */
@SpringBootTest(properties = {
        "jwt.secret=test_secret_key_for_unit_tests_only_not_real_value_32b",
        "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_driver_vehicle_db_test",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.config.import=optional:file:nonexistent.env[.properties]"
})
class DriverReservationIntegrationTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private final List<String> createdDriverIds = new ArrayList<>();
    private final List<String> createdVehicleIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdVehicleIds.forEach(id -> {
            try { vehicleRepository.deleteById(id); } catch (Exception ignored) {}
        });
        createdDriverIds.forEach(id -> {
            try { driverRepository.deleteById(id); } catch (Exception ignored) {}
        });
        createdVehicleIds.clear();
        createdDriverIds.clear();
    }

    @Test
    void onlyOneOfTwoConcurrentRequestsReservesTheSameDriver() throws Exception {
        Driver driver = newAvailableDriver(new ServiceArea("Divulapitiya", "Gampaha"),
                LocalDateTime.now().minusHours(1));
        newActiveVehicle(driver.getId());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<DriverReservationResponse> f1 = pool.submit(() -> {
                start.await();
                return driverService.reserveDriver(driver.getId(), null);
            });
            Future<DriverReservationResponse> f2 = pool.submit(() -> {
                start.await();
                return driverService.reserveDriver(driver.getId(), null);
            });

            start.countDown(); // release both threads at once

            DriverReservationResponse r1 = f1.get(10, TimeUnit.SECONDS);
            DriverReservationResponse r2 = f2.get(10, TimeUnit.SECONDS);

            long successes = (r1.reserved() ? 1 : 0) + (r2.reserved() ? 1 : 0);
            assertEquals(1, successes, "Exactly one concurrent request may reserve the driver");

            Driver after = driverRepository.findById(driver.getId()).orElseThrow();
            assertEquals(AvailabilityStatus.ON_RIDE, after.getAvailabilityStatus());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void secondReservationFailsAfterDriverIsOnRide() {
        Driver driver = newAvailableDriver(new ServiceArea("Colombo", "Colombo"),
                LocalDateTime.now().minusMinutes(30));
        newActiveVehicle(driver.getId());

        DriverReservationResponse first = driverService.reserveDriver(driver.getId(), null);
        DriverReservationResponse second = driverService.reserveDriver(driver.getId(), null);

        assertTrue(first.reserved());
        assertNotNull(first.vehicleId());
        assertFalse(second.reserved(), "A driver already ON_RIDE cannot be reserved again");
    }

    @Test
    void reservationReleasesDriverWhenNoActiveVehicleExists() {
        Driver driver = newAvailableDriver(new ServiceArea("Kandy", "Kandy"),
                LocalDateTime.now().minusMinutes(10));
        // No vehicle created for this driver.

        DriverReservationResponse result = driverService.reserveDriver(driver.getId(), null);

        assertFalse(result.reserved());
        Driver after = driverRepository.findById(driver.getId()).orElseThrow();
        assertEquals(AvailabilityStatus.AVAILABLE, after.getAvailabilityStatus(),
                "Driver must be released back to AVAILABLE when it has no ACTIVE vehicle");
        assertNotNull(after.getAvailableSince());
    }

    @Test
    void serviceAreaFilterExcludesGloballyOlderOutOfAreaDriver() {
        // dA is globally older but OUTSIDE the pickup service area.
        Driver outOfArea = newAvailableDriver(new ServiceArea("Colombo", "Colombo"),
                LocalDateTime.now().minusHours(5));
        newActiveVehicle(outOfArea.getId());
        // dB is younger but INSIDE the pickup service area.
        Driver inArea = newAvailableDriver(new ServiceArea("Divulapitiya", "Gampaha"),
                LocalDateTime.now().minusHours(1));
        newActiveVehicle(inArea.getId());

        List<DriverResponse> eligible = driverService.getEligibleDrivers("Divulapitiya", null);

        assertTrue(eligible.stream().anyMatch(d -> d.id().equals(inArea.getId())),
                "In-area driver must be eligible");
        assertTrue(eligible.stream().noneMatch(d -> d.id().equals(outOfArea.getId())),
                "Globally-older out-of-area driver must NOT be eligible");
    }

    // ---- helpers ----

    private Driver newAvailableDriver(ServiceArea area, LocalDateTime availableSince) {
        Driver driver = new Driver();
        String uid = UUID.randomUUID().toString();
        driver.setAccountId("acc-" + uid);
        driver.setLicenseNumber("LIC-" + uid);
        driver.setFirstName("Test");
        driver.setLastName("Driver");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        driver.setAvailableSince(availableSince);
        driver.setServiceArea(area);
        Driver saved = driverRepository.save(driver);
        createdDriverIds.add(saved.getId());
        return saved;
    }

    private Vehicle newActiveVehicle(String driverId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setDriverId(driverId);
        vehicle.setRegistrationNumber("REG-" + UUID.randomUUID());
        vehicle.setMake("Toyota");
        vehicle.setModel("Prius");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setColor("White");
        vehicle.setManufactureYear(2020);
        vehicle.setSeatingCapacity(4);
        vehicle.setVehicleStatus(VehicleStatus.ACTIVE);
        Vehicle saved = vehicleRepository.save(vehicle);
        createdVehicleIds.add(saved.getId());
        return saved;
    }
}
