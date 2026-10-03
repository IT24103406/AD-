package com.ridelink.ridemanagementservice.client;

import com.ridelink.ridemanagementservice.dto.AvailabilityUpdateRequest;
import com.ridelink.ridemanagementservice.dto.DriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for communicating with Driver & Vehicle Service (Member 2).
 */
@FeignClient(name = "driver-vehicle-service", url = "${services.driver-service.base-url:http://localhost:8082}")
public interface DriverFeignClient {

    @GetMapping("/api/drivers/{id}")
    DriverResponse getDriverById(@PathVariable("id") String id);

    @GetMapping("/api/drivers/account/{accountId}")
    DriverResponse getDriverByAccountId(@PathVariable("accountId") String accountId);

    @PutMapping("/api/drivers/{id}/availability")
    void updateAvailability(@PathVariable("id") String id, @RequestBody AvailabilityUpdateRequest request);

    @GetMapping("/api/drivers/eligible")
    List<DriverResponse> getEligibleDrivers(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "vehicleType", required = false) String vehicleType);
}
