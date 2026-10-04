package com.ridelink.drivervehicleservice.service;

import com.ridelink.drivervehicleservice.client.AccountServiceClient;
import com.ridelink.drivervehicleservice.dto.request.AvailabilityRequest;
import com.ridelink.drivervehicleservice.dto.request.DriverRequest;
import com.ridelink.drivervehicleservice.dto.response.AccountUserResponse;
import com.ridelink.drivervehicleservice.dto.response.DriverResponse;
import com.ridelink.drivervehicleservice.exception.DuplicateResourceException;
import com.ridelink.drivervehicleservice.exception.InvalidAccountException;
import com.ridelink.drivervehicleservice.exception.ResourceNotFoundException;
import com.ridelink.drivervehicleservice.model.Driver;
import com.ridelink.drivervehicleservice.model.enums.AvailabilityStatus;
import com.ridelink.drivervehicleservice.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private DriverService driverService;

    private Driver driver;
    private DriverRequest request;
    private AccountUserResponse accountUser;

    @BeforeEach
    void setUp() {
        driver = new Driver();
        driver.setId("1");
        driver.setAccountId("acc1");
        driver.setLicenseNumber("LIC123");

        request = new DriverRequest("acc1", "John", "Doe", "1234567890", "LIC123", LocalDate.now().plusYears(1));

        accountUser = new AccountUserResponse();
        accountUser.setId("acc1");
        accountUser.setRole("DRIVER");
        accountUser.setStatus("ACTIVE");
    }

    @Test
    void createDriver_Success() {
        when(driverRepository.existsByAccountId(any())).thenReturn(false);
        when(driverRepository.existsByLicenseNumber(any())).thenReturn(false);
        when(accountServiceClient.getUserById("acc1")).thenReturn(accountUser);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        DriverResponse response = driverService.createDriver(request);

        assertNotNull(response);
        assertEquals("acc1", response.accountId());
        verify(accountServiceClient, times(1)).getUserById("acc1");
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void createDriver_DuplicateAccount() {
        when(driverRepository.existsByAccountId(any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> driverService.createDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
        verify(accountServiceClient, never()).getUserById(any());
    }

    @Test
    void createDriver_InvalidRole() {
        accountUser.setRole("PASSENGER");
        when(driverRepository.existsByAccountId(any())).thenReturn(false);
        when(driverRepository.existsByLicenseNumber(any())).thenReturn(false);
        when(accountServiceClient.getUserById("acc1")).thenReturn(accountUser);

        assertThrows(InvalidAccountException.class, () -> driverService.createDriver(request));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void updateAvailability_Success() {
        when(driverRepository.findById("1")).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        DriverResponse response = driverService.updateAvailability("1", new AvailabilityRequest(AvailabilityStatus.AVAILABLE));

        assertNotNull(response);
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void updateAvailability_NotFound() {
        when(driverRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                driverService.updateAvailability("1", new AvailabilityRequest(AvailabilityStatus.AVAILABLE)));
    }
}
