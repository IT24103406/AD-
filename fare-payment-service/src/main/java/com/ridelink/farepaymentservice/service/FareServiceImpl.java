package com.ridelink.farepaymentservice.service;

import com.ridelink.farepaymentservice.client.RideServiceClient;
import com.ridelink.farepaymentservice.dto.FareEstimateRequest;
import com.ridelink.farepaymentservice.dto.FareEstimateResponse;
import com.ridelink.farepaymentservice.dto.FareResponse;
import com.ridelink.farepaymentservice.dto.FinalFareRequest;
import com.ridelink.farepaymentservice.dto.RideServiceResponse;
import com.ridelink.farepaymentservice.exception.FareNotFoundException;
import com.ridelink.farepaymentservice.exception.InvalidRideStateException;
import com.ridelink.farepaymentservice.exception.ResourceConflictException;
import com.ridelink.farepaymentservice.exception.UnauthorizedPaymentAccessException;
import com.ridelink.farepaymentservice.model.Fare;
import com.ridelink.farepaymentservice.repository.FareRepository;
import com.ridelink.farepaymentservice.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FareServiceImpl implements FareService {

    private final FareRepository fareRepository;
    private final RideServiceClient rideServiceClient;
    private final AuthenticatedUserService authService;

    @Value("${fare.base-fare}")
    private BigDecimal baseFare;

    @Value("${fare.per-km-rate}")
    private BigDecimal perKmRate;

    @Value("${fare.per-minute-rate}")
    private BigDecimal perMinuteRate;

    @Value("${fare.minimum-fare}")
    private BigDecimal minimumFare;

    private static final String CURRENCY = "LKR";

    @Override
    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        BigDecimal distanceCharge = BigDecimal.valueOf(request.getDistanceKm()).multiply(perKmRate);
        BigDecimal timeCharge = BigDecimal.valueOf(request.getEstimatedDurationMinutes()).multiply(perMinuteRate);
        
        BigDecimal calculatedFare = baseFare.add(distanceCharge).add(timeCharge);
        BigDecimal estimatedFare = calculatedFare.max(minimumFare).setScale(2, RoundingMode.HALF_UP);

        return FareEstimateResponse.builder()
                .baseFare(baseFare.setScale(2, RoundingMode.HALF_UP))
                .distanceCharge(distanceCharge.setScale(2, RoundingMode.HALF_UP))
                .timeCharge(timeCharge.setScale(2, RoundingMode.HALF_UP))
                .estimatedFare(estimatedFare)
                .currency(CURRENCY)
                .build();
    }

    @Override
    @Transactional
    public FareResponse calculateFinalFare(String rideId, FinalFareRequest request) {
        if (fareRepository.existsByRideId(rideId)) {
            throw new ResourceConflictException("Final fare already exists for ride: " + rideId);
        }

        RideServiceResponse ride = rideServiceClient.getRideById(rideId);
        
        validateOwnership(ride.getPassengerAccountId());

        if (!"COMPLETED".equalsIgnoreCase(ride.getStatus())) {
            throw new InvalidRideStateException("Cannot calculate final fare for ride not in COMPLETED state");
        }

        BigDecimal distanceCharge = BigDecimal.valueOf(request.getActualDistanceKm()).multiply(perKmRate);
        BigDecimal timeCharge = BigDecimal.valueOf(request.getActualDurationMinutes()).multiply(perMinuteRate);
        
        BigDecimal calculatedFare = baseFare.add(distanceCharge).add(timeCharge);
        BigDecimal totalFare = calculatedFare.max(minimumFare).setScale(2, RoundingMode.HALF_UP);

        Fare fare = Fare.builder()
                .rideId(rideId)
                .passengerAccountId(ride.getPassengerAccountId())
                .baseFare(baseFare.setScale(2, RoundingMode.HALF_UP))
                .distanceKm(request.getActualDistanceKm())
                .durationMinutes(request.getActualDurationMinutes())
                .distanceCharge(distanceCharge.setScale(2, RoundingMode.HALF_UP))
                .timeCharge(timeCharge.setScale(2, RoundingMode.HALF_UP))
                .totalFare(totalFare)
                .currency(CURRENCY)
                .calculatedAt(LocalDateTime.now())
                .build();

        fare = fareRepository.save(fare);
        return mapToResponse(fare);
    }

    @Override
    public FareResponse getFareByRideId(String rideId) {
        Fare fare = fareRepository.findByRideId(rideId)
                .orElseThrow(() -> new FareNotFoundException("Fare not found for ride: " + rideId));
                
        validateOwnership(fare.getPassengerAccountId());
        
        return mapToResponse(fare);
    }

    private void validateOwnership(String passengerAccountId) {
        if (!authService.isAdmin() && !passengerAccountId.equals(authService.getCurrentAccountId())) {
            throw new UnauthorizedPaymentAccessException("You do not have permission to access this fare");
        }
    }

    private FareResponse mapToResponse(Fare fare) {
        return FareResponse.builder()
                .id(fare.getId())
                .rideId(fare.getRideId())
                .passengerAccountId(fare.getPassengerAccountId())
                .baseFare(fare.getBaseFare())
                .distanceKm(fare.getDistanceKm())
                .durationMinutes(fare.getDurationMinutes())
                .distanceCharge(fare.getDistanceCharge())
                .timeCharge(fare.getTimeCharge())
                .totalFare(fare.getTotalFare())
                .currency(fare.getCurrency())
                .calculatedAt(fare.getCalculatedAt())
                .build();
    }
}
