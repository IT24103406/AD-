package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.client.AccountServiceClient;
import com.ridelink.drivervehicleservice.dto.request.AvailabilityRequest;
import com.ridelink.drivervehicleservice.dto.request.DriverRequest;
import com.ridelink.drivervehicleservice.dto.request.LocationRequest;
import com.ridelink.drivervehicleservice.dto.request.ServiceAreaRequest;
import com.ridelink.drivervehicleservice.dto.response.AccountUserResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverReservationResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverResponse;
import com.ridelink.drivervehicleservice.exception.DuplicateResourceException;
import com.ridelink.drivervehicleservice.exception.InvalidAccountException;
import com.ridelink.drivervehicleservice.exception.ResourceNotFoundException;
import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.Location;
import com.ridelink.drivervehicleservice.model.ServiceArea;
import com.ridelink.drivervehicleservice.model.Vehicle;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleType;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import com.ridelink.drivervehicleservice.repository.VehicleRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    public DriverResponse createDriver(DriverRequest request) {
        if (driverRepository.existsByAccountId(request.accountId())) {
            throw new DuplicateResourceException("Driver already exists for account ID: " + request.accountId());
        }
        if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new DuplicateResourceException("License number already registered: " + request.licenseNumber());
        }

        validateDriverAccount(request.accountId());

        Driver driver = new Driver();
        driver.setAccountId(request.accountId());
        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setPhone(request.phone());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setLicenseExpiryDate(request.licenseExpiryDate());
        driver.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        Driver saved = driverRepository.save(driver);
        return mapToResponse(saved);
    }

    /**
     * Validates the account via Account Service Feign call.
     * Requires a DRIVER role and preferably ACTIVE status.
     */
    private void validateDriverAccount(String accountId) {
        AccountUserResponse account;
        try {
            account = accountServiceClient.getUserById(accountId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Account not found in Account Service: " + accountId);
        } catch (FeignException e) {
            throw new InvalidAccountException(
                    "Unable to validate account with Account Service (HTTP " + e.status() + "): " + accountId);
        }

        if (account == null || account.getId() == null) {
            throw new ResourceNotFoundException("Account not found in Account Service: " + accountId);
        }

        String role = account.getRole() != null ? account.getRole().trim().toUpperCase() : "";
        if (!"DRIVER".equals(role) && !"ROLE_DRIVER".equals(role)) {
            throw new InvalidAccountException(
                    "Account " + accountId + " must have DRIVER role, but has: " + account.getRole());
        }

        if (account.getStatus() != null) {
            String status = account.getStatus().trim().toUpperCase();
            if (!"ACTIVE".equals(status)) {
                throw new InvalidAccountException(
                        "Account " + accountId + " must be ACTIVE, but has status: " + account.getStatus());
            }
        }
    }

    private Driver findDriverByIdOrAccountId(String id) {
        return driverRepository.findById(id)
                .or(() -> driverRepository.findByAccountId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id or accountId: " + id));
    }

    public DriverResponse getDriver(String id) {
        Driver driver = findDriverByIdOrAccountId(id);
        return mapToResponse(driver);
    }

    public DriverResponse getDriverByAccountId(String accountId) {
        Driver driver = driverRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found for account id: " + accountId));
        return mapToResponse(driver);
    }

    public List<DriverResponse> getAllDrivers(AvailabilityStatus availabilityStatus) {
        List<Driver> drivers;
        if (availabilityStatus != null) {
            drivers = driverRepository.findByAvailabilityStatus(availabilityStatus);
        } else {
            drivers = driverRepository.findAll();
        }
        return drivers.stream().map(this::mapToResponse).toList();
    }

    public DriverResponse updateDriver(String id, DriverRequest request) {
        Driver driver = findDriverByIdOrAccountId(id);

        if (!driver.getLicenseNumber().equals(request.licenseNumber()) &&
            driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new DuplicateResourceException("License number already registered: " + request.licenseNumber());
        }

        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setPhone(request.phone());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setLicenseExpiryDate(request.licenseExpiryDate());

        return mapToResponse(driverRepository.save(driver));
    }

    public DriverResponse updateAvailability(String id, AvailabilityRequest request) {
        Driver driver = findDriverByIdOrAccountId(id);

        driver.setAvailabilityStatus(request.status());
        // A driver only enters the longest-available-first queue when they become AVAILABLE.
        if (request.status() == AvailabilityStatus.AVAILABLE) {
            driver.setAvailableSince(LocalDateTime.now());
        }
        return mapToResponse(driverRepository.save(driver));
    }

    public DriverResponse updateServiceArea(String id, ServiceAreaRequest request) {
        Driver driver = findDriverByIdOrAccountId(id);

        driver.setServiceArea(new ServiceArea(request.city(), request.district()));
        return mapToResponse(driverRepository.save(driver));
    }

    public DriverResponse updateLocation(String id, LocationRequest request) {
        Driver driver = findDriverByIdOrAccountId(id);

        driver.setCurrentLocation(new Location(request.latitude(), request.longitude(), LocalDateTime.now()));
        return mapToResponse(driverRepository.save(driver));
    }

    /**
     * Returns AVAILABLE drivers eligible for automatic assignment.
     *
     * Eligibility rules (applied in this order):
     * 1. Service-area filtering: when a serviceArea is supplied, only drivers whose
     *    serviceArea.city OR serviceArea.district matches it (case-insensitive) are kept.
     * 2. Driver availability status is AVAILABLE.
     * 3. Driver has at least one ACTIVE vehicle (optionally of the requested type).
     *
     * Results are then ordered by availableSince ASC (longest-available first, FIFO).
     * Service-area filtering therefore happens BEFORE the longest-available sorting, so a
     * globally older driver outside the pickup area is never selected over an in-area driver.
     * Drivers without an ACTIVE vehicle are skipped. Each returned driver carries the
     * selected activeVehicleId so callers can assign driver + vehicle together.
     */
    public List<DriverResponse> getEligibleDrivers(String serviceArea, String vehicleType) {
        List<Driver> drivers;
        if (serviceArea != null && !serviceArea.isBlank()) {
            drivers = driverRepository.findEligibleDriversByServiceArea(AvailabilityStatus.AVAILABLE, serviceArea.trim());
        } else {
            drivers = driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }

        VehicleType requestedType = parseVehicleType(vehicleType);

        List<DriverResponse> eligible = new ArrayList<>();
        drivers.stream()
                .sorted(Comparator.comparing(Driver::getAvailableSince,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(driver -> {
                    Vehicle activeVehicle = findActiveVehicle(driver.getId(), requestedType);
                    if (activeVehicle != null) {
                        eligible.add(mapToResponse(driver, activeVehicle.getId()));
                    }
                });

        return eligible;
    }

    /**
     * Atomically reserves a driver for a ride.
     *
     * Uses a conditional findAndModify (WHERE _id = driverId AND availabilityStatus = AVAILABLE
     * SET availabilityStatus = ON_RIDE). Only one concurrent caller can win this transition, so
     * two simultaneous ride requests can never both reserve the same driver. If the driver was
     * already taken, reserved = false and no vehicle lookup happens.
     *
     * After a successful reservation an ACTIVE vehicle is resolved. If the driver has no ACTIVE
     * vehicle, the driver is released back to AVAILABLE (availableSince = now) so they are not
     * left stuck ON_RIDE, and reserved = false is returned.
     */
    public DriverReservationResponse reserveDriver(String driverId, String vehicleType) {
        Query query = new Query(Criteria.where("id").is(driverId)
                .and("availabilityStatus").is(AvailabilityStatus.AVAILABLE));
        Update update = new Update()
                .set("availabilityStatus", AvailabilityStatus.ON_RIDE)
                .unset("availableSince");

        Driver reserved = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Driver.class);

        if (reserved == null) {
            // Another request already flipped this driver out of AVAILABLE.
            return DriverReservationResponse.failure(driverId, "Driver is not AVAILABLE (already reserved)");
        }

        Vehicle activeVehicle = findActiveVehicle(driverId, parseVehicleType(vehicleType));
        if (activeVehicle == null) {
            // No usable vehicle: release the driver back to the availability queue.
            releaseDriver(driverId);
            return DriverReservationResponse.failure(driverId, "No ACTIVE vehicle for driver");
        }

        return DriverReservationResponse.success(driverId, activeVehicle.getId());
    }

    /**
     * Releases a driver back to AVAILABLE and re-stamps availableSince so they rejoin the
     * end of the longest-available-first queue.
     */
    private void releaseDriver(String driverId) {
        Query query = new Query(Criteria.where("id").is(driverId));
        Update update = new Update()
                .set("availabilityStatus", AvailabilityStatus.AVAILABLE)
                .set("availableSince", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Driver.class);
    }

    private VehicleType parseVehicleType(String vehicleType) {
        if (vehicleType == null || vehicleType.isBlank()) {
            return null;
        }
        try {
            return VehicleType.valueOf(vehicleType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Vehicle findActiveVehicle(String driverId, VehicleType vehicleType) {
        List<Vehicle> vehicles = (vehicleType != null)
                ? vehicleRepository.findByDriverIdAndVehicleTypeAndVehicleStatus(driverId, vehicleType, VehicleStatus.ACTIVE)
                : vehicleRepository.findByDriverIdAndVehicleStatus(driverId, VehicleStatus.ACTIVE);
        return (vehicles == null || vehicles.isEmpty()) ? null : vehicles.get(0);
    }

    private DriverResponse mapToResponse(Driver driver) {
        return mapToResponse(driver, null);
    }

    private DriverResponse mapToResponse(Driver driver, String activeVehicleId) {
        return new DriverResponse(
                driver.getId(),
                driver.getAccountId(),
                driver.getFirstName(),
                driver.getLastName(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiryDate(),
                driver.getAvailabilityStatus(),
                driver.getAvailableSince(),
                activeVehicleId,
                driver.getServiceArea(),
                driver.getCurrentLocation(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }
}
