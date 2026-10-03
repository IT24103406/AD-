package com.ridelink.farepaymentservice.service;

import com.ridelink.farepaymentservice.dto.PaymentRequest;
import com.ridelink.farepaymentservice.dto.PaymentResponse;
import com.ridelink.farepaymentservice.dto.ReceiptResponse;
import com.ridelink.farepaymentservice.exception.FareNotFoundException;
import com.ridelink.farepaymentservice.exception.InvalidRideStateException;
import com.ridelink.farepaymentservice.exception.PaymentAlreadyCompletedException;
import com.ridelink.farepaymentservice.exception.PaymentNotFoundException;
import com.ridelink.farepaymentservice.exception.UnauthorizedPaymentAccessException;
import com.ridelink.farepaymentservice.model.Fare;
import com.ridelink.farepaymentservice.model.Payment;
import com.ridelink.farepaymentservice.model.PaymentStatus;
import com.ridelink.farepaymentservice.repository.FareRepository;
import com.ridelink.farepaymentservice.repository.PaymentRepository;
import com.ridelink.farepaymentservice.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;
    private final AuthenticatedUserService authService;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Fare fare = fareRepository.findByRideId(request.getRideId())
                .orElseThrow(() -> new FareNotFoundException("Final fare not found for ride. Cannot process payment."));

        validateOwnership(fare.getPassengerAccountId());

        if (paymentRepository.existsByRideIdAndStatus(request.getRideId(), PaymentStatus.PAID)) {
            throw new PaymentAlreadyCompletedException("Ride has already been successfully paid");
        }

        String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = Payment.builder()
                .rideId(fare.getRideId())
                .fareId(fare.getId())
                .passengerAccountId(fare.getPassengerAccountId())
                .amount(fare.getTotalFare())
                .currency(fare.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PAID) // Simulated deterministic success
                .transactionReference(txnRef)
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
                
        validateOwnership(payment.getPassengerAccountId());
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByRideId(String rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for ride"));
                
        validateOwnership(payment.getPassengerAccountId());
        return mapToResponse(payment);
    }

    @Override
    public ReceiptResponse generateReceipt(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        validateOwnership(payment.getPassengerAccountId());

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InvalidRideStateException("Receipt is only available for PAID payments");
        }

        String receiptNumber = "RCP-" + payment.getTransactionReference().split("-")[1];

        return ReceiptResponse.builder()
                .receiptNumber(receiptNumber)
                .transactionReference(payment.getTransactionReference())
                .rideId(payment.getRideId())
                .passengerAccountId(payment.getPassengerAccountId())
                .fareAmount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getStatus())
                .currency(payment.getCurrency())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private void validateOwnership(String passengerAccountId) {
        if (!authService.isAdmin() && !passengerAccountId.equals(authService.getCurrentAccountId())) {
            throw new UnauthorizedPaymentAccessException("You do not have permission to access this payment");
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .rideId(payment.getRideId())
                .fareId(payment.getFareId())
                .passengerAccountId(payment.getPassengerAccountId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .failedAt(payment.getFailedAt())
                .build();
    }
}
