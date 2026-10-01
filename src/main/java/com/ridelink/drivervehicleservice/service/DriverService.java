package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.client.AccountServiceClient;
import com.ridelink.drivervehicleservice.dto.request.AvailabilityRequest;
import com.ridelink.drivervehicleservice.dto.request.DriverRequest;
import com.ridelink.drivervehicleservice.dto.request.LocationRequest;
import com.ridelink.drivervehicleservice.dto.request.ServiceAreaRequest;
import com.ridelink.drivervehicleservice.dto.response.AccountUserResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverResponse;
import com.ridelink.drivervehicleservice.exception.DuplicateResourceException;
import com.ridelink.drivervehicleservice.exception.InvalidAccountException;
import com.ridelink.drivervehicleservice.exception.ResourceNotFoundException;
import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.Location;
import com.ridelink.drivervehicleservice.model.ServiceArea;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

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

    public List<DriverResponse> getEligibleDrivers(String city, String vehicleType) {
        List<Driver> drivers;
        if (city != null && !city.isEmpty()) {
            drivers = driverRepository.findEligibleDriversByCity(AvailabilityStatus.AVAILABLE, city);
        } else {
            drivers = driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }

        return drivers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private DriverResponse mapToResponse(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getAccountId(),
                driver.getFirstName(),
                driver.getLastName(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiryDate(),
                driver.getAvailabilityStatus(),
                driver.getServiceArea(),
                driver.getCurrentLocation(),
                driver.getCreatedAt(),
                driver.getUpdatedAt()
        );
    }
}
