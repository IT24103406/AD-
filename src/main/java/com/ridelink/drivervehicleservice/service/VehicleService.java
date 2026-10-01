package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.dto.request.VehicleRequest;
import com.ridelink.drivervehicleservice.dto.response.VehicleResponse;
import com.ridelink.drivervehicleservice.exception.DuplicateResourceException;
import com.ridelink.drivervehicleservice.exception.ResourceNotFoundException;
import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.Vehicle;
import com.ridelink.drivervehicleservice.model.enums.VehicleStatus;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import com.ridelink.drivervehicleservice.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    private String resolveDriverId(String driverId) {
        if (driverRepository.existsById(driverId)) {
            return driverId;
        }
        return driverRepository.findByAccountId(driverId)
                .map(Driver::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id or accountId: " + driverId));
    }

    public VehicleResponse addVehicle(String driverId, VehicleRequest request) {
        String effectiveDriverId = resolveDriverId(driverId);

        if (vehicleRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.registrationNumber());
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setDriverId(effectiveDriverId);
        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setColor(request.color());
        vehicle.setManufactureYear(request.manufactureYear());
        vehicle.setSeatingCapacity(request.seatingCapacity());
        vehicle.setVehicleStatus(VehicleStatus.ACTIVE);

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    public List<VehicleResponse> getVehiclesByDriver(String driverId) {
        String effectiveDriverId = driverId;
        if (!driverRepository.existsById(driverId)) {
            effectiveDriverId = driverRepository.findByAccountId(driverId)
                    .map(Driver::getId)
                    .orElse(driverId);
        }
        return vehicleRepository.findByDriverId(effectiveDriverId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VehicleResponse getVehicle(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        return mapToResponse(vehicle);
    }

    public VehicleResponse updateVehicle(String vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        if (!vehicle.getRegistrationNumber().equals(request.registrationNumber()) &&
            vehicleRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.registrationNumber());
        }

        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setColor(request.color());
        vehicle.setManufactureYear(request.manufactureYear());
        vehicle.setSeatingCapacity(request.seatingCapacity());

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDriverId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getVehicleType(),
                vehicle.getColor(),
                vehicle.getManufactureYear(),
                vehicle.getSeatingCapacity(),
                vehicle.getVehicleStatus(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}
