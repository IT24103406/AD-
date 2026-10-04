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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    @Mock
    private RideServiceClient rideServiceClient;

    @Mock
    private AuthenticatedUserService authService;

    @InjectMocks
    private FareServiceImpl fareService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fareService, "baseFare", new BigDecimal("150.00"));
        ReflectionTestUtils.setField(fareService, "perKmRate", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(fareService, "perMinuteRate", new BigDecimal("5.00"));
        ReflectionTestUtils.setField(fareService, "minimumFare", new BigDecimal("250.00"));
    }

    @Test
    void estimateFare_ShouldCalculateCorrectly() {
        FareEstimateRequest req = new FareEstimateRequest();
        req.setDistanceKm(10.0);
        req.setEstimatedDurationMinutes(20);

        FareEstimateResponse res = fareService.estimateFare(req);

        assertEquals(new BigDecimal("150.00"), res.getBaseFare());
        assertEquals(new BigDecimal("800.00"), res.getDistanceCharge());
        assertEquals(new BigDecimal("100.00"), res.getTimeCharge());
        assertEquals(new BigDecimal("1050.00"), res.getEstimatedFare());
    }

    @Test
    void estimateFare_ShouldApplyMinimumFare() {
        FareEstimateRequest req = new FareEstimateRequest();
        req.setDistanceKm(0.5); // 40
        req.setEstimatedDurationMinutes(5); // 25
        // 150 + 40 + 25 = 215 < 250(min)

        FareEstimateResponse res = fareService.estimateFare(req);
        assertEquals(new BigDecimal("250.00"), res.getEstimatedFare());
    }

    @Test
    void calculateFinalFare_ShouldSucceedForCompletedRide() {
        String rideId = "ride-1";
        FinalFareRequest req = new FinalFareRequest();
        req.setActualDistanceKm(10.0);
        req.setActualDurationMinutes(20);

        RideServiceResponse ride = new RideServiceResponse();
        ride.setId(rideId);
        ride.setPassengerAccountId("p-1");
        ride.setStatus("COMPLETED");

        when(fareRepository.existsByRideId(rideId)).thenReturn(false);
        when(rideServiceClient.getRideById(rideId)).thenReturn(ride);
        when(authService.getCurrentAccountId()).thenReturn("p-1");
        when(authService.isAdmin()).thenReturn(false);
        
        Fare savedFare = Fare.builder()
                .id("fare-1")
                .rideId(rideId)
                .passengerAccountId("p-1")
                .totalFare(new BigDecimal("1050.00"))
                .build();
        when(fareRepository.save(any(Fare.class))).thenReturn(savedFare);

        FareResponse res = fareService.calculateFinalFare(rideId, req);

        assertNotNull(res);
        assertEquals("fare-1", res.getId());
        verify(fareRepository).save(any(Fare.class));
    }

    @Test
    void calculateFinalFare_ShouldRejectDuplicate() {
        String rideId = "ride-1";
        FinalFareRequest req = new FinalFareRequest();

        when(fareRepository.existsByRideId(rideId)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> fareService.calculateFinalFare(rideId, req));
    }

    @Test
    void calculateFinalFare_ShouldRejectInvalidRideState() {
        String rideId = "ride-1";
        FinalFareRequest req = new FinalFareRequest();

        RideServiceResponse ride = new RideServiceResponse();
        ride.setId(rideId);
        ride.setPassengerAccountId("p-1");
        ride.setStatus("IN_PROGRESS");

        when(fareRepository.existsByRideId(rideId)).thenReturn(false);
        when(rideServiceClient.getRideById(rideId)).thenReturn(ride);
        when(authService.getCurrentAccountId()).thenReturn("p-1");

        assertThrows(InvalidRideStateException.class, () -> fareService.calculateFinalFare(rideId, req));
    }

    @Test
    void getFareByRideId_ShouldSucceedForOwner() {
        String rideId = "ride-1";
        Fare fare = Fare.builder().rideId(rideId).passengerAccountId("p-1").build();

        when(fareRepository.findByRideId(rideId)).thenReturn(Optional.of(fare));
        when(authService.getCurrentAccountId()).thenReturn("p-1");

        FareResponse res = fareService.getFareByRideId(rideId);
        assertEquals(rideId, res.getRideId());
    }

    @Test
    void getFareByRideId_ShouldThrowNotFound() {
        when(fareRepository.findByRideId("r-1")).thenReturn(Optional.empty());
        assertThrows(FareNotFoundException.class, () -> fareService.getFareByRideId("r-1"));
    }
    
    @Test
    void getFareByRideId_ShouldThrowUnauthorizedForDifferentUser() {
        String rideId = "ride-1";
        Fare fare = Fare.builder().rideId(rideId).passengerAccountId("p-1").build();

        when(fareRepository.findByRideId(rideId)).thenReturn(Optional.of(fare));
        when(authService.isAdmin()).thenReturn(false);
        when(authService.getCurrentAccountId()).thenReturn("p-2");
        
        assertThrows(UnauthorizedPaymentAccessException.class, () -> fareService.getFareByRideId(rideId));
    }
}
