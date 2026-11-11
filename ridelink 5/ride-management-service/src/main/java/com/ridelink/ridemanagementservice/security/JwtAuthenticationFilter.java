package com.ridelink.ridemanagementservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter that intercepts every request, extracts the JWT from the Authorization header,
 * validates it, and populates the Spring SecurityContext with the authenticated user's
 * identity and authorities (roles).
 *
 * This filter VALIDATES tokens from Account Service; it does NOT issue tokens.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (jwtService.isTokenValid(token)) {
                String accountId = jwtService.extractAccountId(token);
                String role = jwtService.extractRole(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (StringUtils.hasText(role)) {
                    // Ensure role has ROLE_ prefix for Spring Security
                    String springRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(springRole));
                }

                // Principal is the accountId; credentials are null (stateless JWT)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(accountId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user '{}' with role '{}'", accountId, role);
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed for request to {}: {}", request.getRequestURI(), e.getMessage());
            // Do not set authentication; Spring Security will return 401
        }

        filterChain.doFilter(request, response);
    }
}
