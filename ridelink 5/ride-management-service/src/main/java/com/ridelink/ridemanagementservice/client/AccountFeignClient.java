package com.ridelink.ridemanagementservice.client;

import com.ridelink.ridemanagementservice.dto.AccountUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with Account Service (Member 1).
 */
@FeignClient(name = "account-service", url = "${services.account-service.base-url:http://localhost:8081}")
public interface AccountFeignClient {

    @GetMapping("/api/users/{id}")
    AccountUserResponse getUserById(@PathVariable("id") String id);

    @GetMapping("/api/users/me")
    AccountUserResponse getMyProfile();
}
