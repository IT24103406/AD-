package com.ridelink.drivervehicleservice.repository;

import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {
    Optional<Driver> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
    boolean existsByLicenseNumber(String licenseNumber);

    @Query("{ 'availabilityStatus': ?0, 'serviceArea.city': { $regex: ?1, $options: 'i' } }")
    List<Driver> findEligibleDriversByCity(AvailabilityStatus status, String city);

    List<Driver> findByAvailabilityStatus(AvailabilityStatus status);
}
