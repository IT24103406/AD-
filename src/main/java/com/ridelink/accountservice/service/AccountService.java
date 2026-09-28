package com.ridelink.accountservice.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridelink.accountservice.dto.RegisterRequest;
import com.ridelink.accountservice.model.AccountStatus;
import com.ridelink.accountservice.model.Role;
import com.ridelink.accountservice.model.User;
import com.ridelink.accountservice.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterRequest request) {

        // Check whether the email is already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.ridelink.accountservice.exception.DuplicateEmailException("Email is already registered");
        }

        // Prevent users from registering themselves as ADMIN
        if (request.getRole() == Role.ADMIN) {
            throw new com.ridelink.accountservice.exception.ForbiddenAccessException("Admin registration is not allowed");
        }

        LocalDateTime now = LocalDateTime.now();

        // Create a new user
        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // Save the user in MongoDB
        return userRepository.save(user);
    }

    public com.ridelink.accountservice.dto.JwtResponse login(com.ridelink.accountservice.dto.LoginRequest request, org.springframework.security.authentication.AuthenticationManager authenticationManager, com.ridelink.accountservice.security.JwtService jwtService) {
        
        authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.ridelink.accountservice.exception.UserNotFoundException("User not found"));

        if (user.getStatus() == AccountStatus.INACTIVE || user.getStatus() == AccountStatus.SUSPENDED) {
            throw new com.ridelink.accountservice.exception.ForbiddenAccessException("Account is " + user.getStatus().name());
        }

        org.springframework.security.core.userdetails.UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        String jwtToken = jwtService.generateToken(userDetails);
        
        return new com.ridelink.accountservice.dto.JwtResponse(jwtToken);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.ridelink.accountservice.exception.UserNotFoundException("User not found with email: " + email));
    }

    public User updateProfile(String email, com.ridelink.accountservice.dto.UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public User updateRole(String id, com.ridelink.accountservice.dto.RoleUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.ridelink.accountservice.exception.UserNotFoundException("User not found with ID: " + id));

        user.setRole(request.getRole());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public User updateStatus(String id, com.ridelink.accountservice.dto.StatusUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.ridelink.accountservice.exception.UserNotFoundException("User not found with ID: " + id));

        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}