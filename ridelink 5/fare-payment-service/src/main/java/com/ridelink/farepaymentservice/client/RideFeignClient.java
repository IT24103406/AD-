package com.ridelink.farepaymentservice.client;

import com.ridelink.farepaymentservice.dto.RideServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with Ride Management Service (Member 3).
 */
@FeignClient(name = "ride-management-service", url = "${services.ride-service.base-url:http://localhost:8083}")
public interface RideFeignClient {

    @GetMapping("/api/rides/{rideId}")
    RideServiceResponse getRideById(@PathVariable("rideId") String rideId);
}
