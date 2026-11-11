package com.ridelink.drivervehicleservice.repository;

import com.ridelink.drivervehicleservice.model.Vehicle;
import com.ridelink.drivervehicleservice.model.enums.VehicleStatus;
import com.ridelink.drivervehicleservice.model.enums.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    boolean existsByRegistrationNumber(String registrationNumber);
    List<Vehicle> findByDriverId(String driverId);
    List<Vehicle> findByDriverIdAndVehicleStatus(String driverId, VehicleStatus status);
    List<Vehicle> findByDriverIdAndVehicleTypeAndVehicleStatus(String driverId, VehicleType type, VehicleStatus status);
}
