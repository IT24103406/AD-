package com.ridelink.farepaymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FarePaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarePaymentServiceApplication.class, args);
    }
}
