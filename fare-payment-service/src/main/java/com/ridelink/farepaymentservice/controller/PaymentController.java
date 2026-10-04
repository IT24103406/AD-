package com.ridelink.farepaymentservice.controller;

import com.ridelink.farepaymentservice.dto.PaymentRequest;
import com.ridelink.farepaymentservice.dto.PaymentResponse;
import com.ridelink.farepaymentservice.dto.ReceiptResponse;
import com.ridelink.farepaymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "Endpoints for simulated payment processing and receipts")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Process a simulated payment for a ride")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        return new ResponseEntity<>(paymentService.processPayment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details by payment ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/rides/{rideId}")
    @Operation(summary = "Get payment details for a specific ride")
    public ResponseEntity<PaymentResponse> getPaymentByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Get receipt for a successful payment")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.generateReceipt(paymentId));
    }
}
