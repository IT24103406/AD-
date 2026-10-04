package com.ridelink.farepaymentservice.service;

import com.ridelink.farepaymentservice.dto.PaymentRequest;
import com.ridelink.farepaymentservice.dto.PaymentResponse;
import com.ridelink.farepaymentservice.dto.ReceiptResponse;
import com.ridelink.farepaymentservice.exception.FareNotFoundException;
import com.ridelink.farepaymentservice.exception.InvalidRideStateException;
import com.ridelink.farepaymentservice.exception.PaymentAlreadyCompletedException;
import com.ridelink.farepaymentservice.exception.PaymentNotFoundException;
import com.ridelink.farepaymentservice.model.Fare;
import com.ridelink.farepaymentservice.model.Payment;
import com.ridelink.farepaymentservice.model.PaymentMethod;
import com.ridelink.farepaymentservice.model.PaymentStatus;
import com.ridelink.farepaymentservice.repository.FareRepository;
import com.ridelink.farepaymentservice.repository.PaymentRepository;
import com.ridelink.farepaymentservice.security.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FareRepository fareRepository;

    @Mock
    private AuthenticatedUserService authService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void processPayment_ShouldSucceed() {
        PaymentRequest req = new PaymentRequest();
        req.setRideId("r-1");
        req.setPaymentMethod(PaymentMethod.CARD);

        Fare fare = Fare.builder()
                .id("f-1").rideId("r-1").passengerAccountId("p-1").totalFare(new BigDecimal("1050.00"))
                .build();

        when(fareRepository.findByRideId("r-1")).thenReturn(Optional.of(fare));
        when(authService.getCurrentAccountId()).thenReturn("p-1");
        when(paymentRepository.existsByRideIdAndStatus("r-1", PaymentStatus.PAID)).thenReturn(false);

        Payment saved = Payment.builder()
                .id("p-1").rideId("r-1").status(PaymentStatus.PAID).transactionReference("TXN-123")
                .build();
        when(paymentRepository.save(any())).thenReturn(saved);

        PaymentResponse res = paymentService.processPayment(req);
        assertEquals("p-1", res.getId());
        assertEquals(PaymentStatus.PAID, res.getStatus());
    }

    @Test
    void processPayment_ShouldThrowFareNotFound() {
        PaymentRequest req = new PaymentRequest();
        req.setRideId("r-1");

        when(fareRepository.findByRideId("r-1")).thenReturn(Optional.empty());

        assertThrows(FareNotFoundException.class, () -> paymentService.processPayment(req));
    }

    @Test
    void processPayment_ShouldRejectDuplicate() {
        PaymentRequest req = new PaymentRequest();
        req.setRideId("r-1");

        Fare fare = Fare.builder().rideId("r-1").passengerAccountId("p-1").build();

        when(fareRepository.findByRideId("r-1")).thenReturn(Optional.of(fare));
        when(authService.getCurrentAccountId()).thenReturn("p-1");
        when(paymentRepository.existsByRideIdAndStatus("r-1", PaymentStatus.PAID)).thenReturn(true);

        assertThrows(PaymentAlreadyCompletedException.class, () -> paymentService.processPayment(req));
    }

    @Test
    void getPaymentById_ShouldSucceed() {
        Payment payment = Payment.builder().id("p-1").passengerAccountId("acc-1").build();
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(payment));
        when(authService.getCurrentAccountId()).thenReturn("acc-1");

        PaymentResponse res = paymentService.getPaymentById("p-1");
        assertEquals("p-1", res.getId());
    }

    @Test
    void getPaymentById_ShouldThrowNotFound() {
        when(paymentRepository.findById("p-1")).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById("p-1"));
    }

    @Test
    void generateReceipt_ShouldSucceedForPaid() {
        Payment payment = Payment.builder()
                .id("p-1")
                .status(PaymentStatus.PAID)
                .transactionReference("TXN-ABCD")
                .passengerAccountId("acc-1")
                .build();
                
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(payment));
        when(authService.getCurrentAccountId()).thenReturn("acc-1");

        ReceiptResponse res = paymentService.generateReceipt("p-1");
        assertEquals("RCP-ABCD", res.getReceiptNumber());
    }

    @Test
    void generateReceipt_ShouldFailForUnpaid() {
        Payment payment = Payment.builder()
                .id("p-1")
                .status(PaymentStatus.PENDING)
                .passengerAccountId("acc-1")
                .build();
                
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(payment));
        when(authService.getCurrentAccountId()).thenReturn("acc-1");

        assertThrows(InvalidRideStateException.class, () -> paymentService.generateReceipt("p-1"));
    }
}
