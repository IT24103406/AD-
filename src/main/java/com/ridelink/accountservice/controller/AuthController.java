package com.ridelink.accountservice.controller;

import com.ridelink.accountservice.dto.RegisterRequest;
import com.ridelink.accountservice.dto.UserResponse;
import com.ridelink.accountservice.model.User;
import com.ridelink.accountservice.service.AccountService;
import com.ridelink.accountservice.security.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AccountService accountService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        User registeredUser = accountService.registerUser(request);

        UserResponse response = new UserResponse(
                registeredUser.getId(),
                registeredUser.getFirstName(),
                registeredUser.getLastName(),
                registeredUser.getEmail(),
                registeredUser.getPhone(),
                registeredUser.getRole(),
                registeredUser.getStatus(),
                registeredUser.getCreatedAt(),
                registeredUser.getUpdatedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<com.ridelink.accountservice.dto.JwtResponse> login(
            @Valid @RequestBody com.ridelink.accountservice.dto.LoginRequest request) {
        
        com.ridelink.accountservice.dto.JwtResponse response = accountService.login(request, authenticationManager, jwtService);
        
        return ResponseEntity.ok(response);
    }
}