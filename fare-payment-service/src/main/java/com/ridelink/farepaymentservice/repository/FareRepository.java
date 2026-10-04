package com.ridelink.farepaymentservice.repository;

import com.ridelink.farepaymentservice.model.Fare;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FareRepository extends MongoRepository<Fare, String> {
    Optional<Fare> findByRideId(String rideId);
    boolean existsByRideId(String rideId);
}
