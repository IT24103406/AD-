package com.ridelink.farepaymentservice.repository;

import com.ridelink.farepaymentservice.model.Payment;
import com.ridelink.farepaymentservice.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByRideId(String rideId);
    Optional<Payment> findByTransactionReference(String transactionReference);
    boolean existsByRideIdAndStatus(String rideId, PaymentStatus status);
}
