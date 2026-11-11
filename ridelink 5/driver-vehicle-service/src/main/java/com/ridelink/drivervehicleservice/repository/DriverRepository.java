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

    /**
     * AVAILABLE drivers whose service area (city OR district) matches the given pickup area
     * (case-insensitive). Service-area filtering is applied before longest-available sorting.
     */
    @Query("{ 'availabilityStatus': ?0, $or: [ " +
            "{ 'serviceArea.city': { $regex: ?1, $options: 'i' } }, " +
            "{ 'serviceArea.district': { $regex: ?1, $options: 'i' } } ] }")
    List<Driver> findEligibleDriversByServiceArea(AvailabilityStatus status, String serviceArea);

    List<Driver> findByAvailabilityStatus(AvailabilityStatus status);
}
