package com.ridelink.farepaymentservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    public String getCurrentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return (String) auth.getPrincipal();
        }
        return null;
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().equals("ROLE_" + role)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isPassenger() {
        return hasRole("PASSENGER");
    }
}
