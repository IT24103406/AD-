package com.ridelink.ridemanagementservice.service;

import com.ridelink.ridemanagementservice.client.DriverFeignClient;
import com.ridelink.ridemanagementservice.dto.AvailabilityUpdateRequest;
import com.ridelink.ridemanagementservice.dto.DriverResponse;
import com.ridelink.ridemanagementservice.exception.ExternalServiceException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service client for communicating with Driver & Vehicle Service (Member 2),
 * powered by Spring Cloud OpenFeign (DriverFeignClient).
 */
@Service
public class DriverServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DriverServiceClient.class);

    private final DriverFeignClient driverFeignClient;

    public DriverServiceClient(DriverFeignClient driverFeignClient) {
        this.driverFeignClient = driverFeignClient;
    }

    /**
     * Fetch a driver by their Driver Service entity ID.
     * Calls: GET /api/drivers/{driverId}
     */
    public DriverResponse getDriverById(String driverId) {
        try {
            return driverFeignClient.getDriverById(driverId);
        } catch (FeignException.NotFound e) {
            log.warn("Driver not found for driverId '{}'", driverId);
            return null;
        } catch (FeignException e) {
            log.error("Driver Service error for driverId '{}': {}", driverId, e.getMessage());
            throw new ExternalServiceException(
                    "Driver Service returned error for driverId: " + driverId, e);
        }
    }

    /**
     * Fetch a driver by their Account Service account ID.
     * Calls: GET /api/drivers/account/{accountId}
     */
    public DriverResponse getDriverByAccountId(String accountId) {
        try {
            return driverFeignClient.getDriverByAccountId(accountId);
        } catch (FeignException.NotFound e) {
            log.warn("Driver not found for accountId '{}'", accountId);
            return null;
        } catch (FeignException e) {
            log.warn("Could not fetch driver for accountId '{}': {}", accountId, e.getMessage());
            return null;
        }
    }

    /**
     * Update a driver's availability status.
     * Calls: PUT /api/drivers/{driverId}/availability
     */
    public void updateDriverAvailability(String driverId, String availabilityStatus) {
        try {
            driverFeignClient.updateAvailability(driverId, new AvailabilityUpdateRequest(availabilityStatus));
            log.debug("Updated driver '{}' availability to '{}'", driverId, availabilityStatus);
        } catch (FeignException e) {
            log.warn("Could not update driver '{}' availability to '{}': {}",
                    driverId, availabilityStatus, e.getMessage());
        }
    }
}

