package com.ridelink.drivervehicleservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DriverVehicleServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriverVehicleServiceApplication.class, args);
	}

}
