package com.ridelink.ridemanagementservice.client;

import com.ridelink.ridemanagementservice.dto.FareEstimateRequest;
import com.ridelink.ridemanagementservice.dto.FareEstimateResponse;
import com.ridelink.ridemanagementservice.dto.FareResponse;
import com.ridelink.ridemanagementservice.dto.FinalFareRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for communicating with Fare & Payment Service (Member 4).
 */
@FeignClient(name = "fare-payment-service", url = "${services.fare-payment-service.base-url:http://localhost:8084}")
public interface FareFeignClient {

    @PostMapping("/api/fares/estimate")
    FareEstimateResponse estimateFare(@RequestBody FareEstimateRequest request);

    @PostMapping("/api/fares/rides/{rideId}/final")
    FareResponse calculateFinalFare(@PathVariable("rideId") String rideId, @RequestBody FinalFareRequest request);

    @GetMapping("/api/fares/rides/{rideId}")
    FareResponse getFareByRideId(@PathVariable("rideId") String rideId);
}
