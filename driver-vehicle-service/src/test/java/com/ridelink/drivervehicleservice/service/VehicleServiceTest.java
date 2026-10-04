package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.dto.request.VehicleRequest;
import com.ridelink.drivervehicleservice.dto.response.VehicleResponse;
import com.ridelink.drivervehicleservice.exception.DuplicateResourceException;
import com.ridelink.drivervehicleservice.exception.ResourceNotFoundException;
import com.ridelink.drivervehicleservice.model.Vehicle;
import com.ridelink.drivervehicleservice.model.enums.VehicleType;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import com.ridelink.drivervehicleservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle vehicle;
    private VehicleRequest request;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId("1");
        vehicle.setDriverId("driver1");
        vehicle.setRegistrationNumber("REG123");

        request = new VehicleRequest("REG123", "Toyota", "Prius", VehicleType.CAR, "White", 2020, 4);
    }

    @Test
    void addVehicle_Success() {
        when(driverRepository.existsById("driver1")).thenReturn(true);
        when(vehicleRepository.existsByRegistrationNumber(any())).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        VehicleResponse response = vehicleService.addVehicle("driver1", request);

        assertNotNull(response);
        assertEquals("driver1", response.driverId());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void addVehicle_DriverNotFound() {
        when(driverRepository.existsById("driver1")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> vehicleService.addVehicle("driver1", request));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void addVehicle_DuplicateRegistration() {
        when(driverRepository.existsById("driver1")).thenReturn(true);
        when(vehicleRepository.existsByRegistrationNumber(any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> vehicleService.addVehicle("driver1", request));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
