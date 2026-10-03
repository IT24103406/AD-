package com.ridelink.farepaymentservice.client;

import com.ridelink.farepaymentservice.dto.RideServiceResponse;
import com.ridelink.farepaymentservice.exception.ExternalServiceException;
import com.ridelink.farepaymentservice.exception.RideNotFoundException;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class RideServiceClient {

    private final RideFeignClient rideFeignClient;

    public RideServiceClient(RideFeignClient rideFeignClient) {
        this.rideFeignClient = rideFeignClient;
    }

    public RideServiceResponse getRideById(String rideId) {
        try {
            return rideFeignClient.getRideById(rideId);
        } catch (FeignException.NotFound e) {
            throw new RideNotFoundException("Ride not found in Ride Management Service");
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new ExternalServiceException("Client error from Ride Service: " + e.status());
            }
            throw new ExternalServiceException("Server error from Ride Service: " + e.status());
        }
    }
}

