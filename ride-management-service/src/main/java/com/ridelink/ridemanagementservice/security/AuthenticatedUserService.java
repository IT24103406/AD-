package com.ridelink.ridemanagementservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Helper service for extracting the authenticated user's identity and role
 * from the Spring SecurityContext, which is populated by JwtAuthenticationFilter.
 *
 * The principal stored in the SecurityContext is the account ID (String)
 * extracted from the JWT's "id" claim by JwtAuthenticationFilter.
 */
@Service
public class AuthenticatedUserService {

    /**
     * Get the current authenticated account ID.
     *
     * @return the account ID string from the JWT, or null if not authenticated
     */
    public String getCurrentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal instanceof String ? (String) principal : null;
    }

    /**
     * Get the primary role of the current authenticated user.
     * Returns the role WITHOUT the ROLE_ prefix (e.g., "PASSENGER", "DRIVER", "ADMIN").
     */
    public String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .findFirst()
                .map(a -> {
                    String authority = a.getAuthority();
                    return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
                })
                .orElse(null);
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param role role name without ROLE_ prefix (e.g., "ADMIN")
     */
    public boolean hasRole(String role) {
        String currentRole = getCurrentRole();
        return role.equalsIgnoreCase(currentRole);
    }

    /**
     * Check whether the current user is an ADMIN.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check whether the current user is a PASSENGER.
     */
    public boolean isPassenger() {
        return hasRole("PASSENGER");
    }

    /**
     * Check whether the current user is a DRIVER.
     */
    public boolean isDriver() {
        return hasRole("DRIVER");
    }
}
