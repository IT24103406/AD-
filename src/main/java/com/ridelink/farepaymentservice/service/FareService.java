package com.ridelink.farepaymentservice.service;

import com.ridelink.farepaymentservice.dto.FareEstimateRequest;
import com.ridelink.farepaymentservice.dto.FareEstimateResponse;
import com.ridelink.farepaymentservice.dto.FareResponse;
import com.ridelink.farepaymentservice.dto.FinalFareRequest;

public interface FareService {
    FareEstimateResponse estimateFare(FareEstimateRequest request);
    FareResponse calculateFinalFare(String rideId, FinalFareRequest request);
    FareResponse getFareByRideId(String rideId);
}
