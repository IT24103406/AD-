package com.ridelink.ridemanagementservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 *
 * Access Swagger at: http://localhost:8083/swagger-ui/index.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "RideLink - Ride Management Service",
        version = "1.0.0",
        description = "Ride Management Microservice for RideLink platform. " +
                      "Handles ride lifecycle: creation, driver assignment, ride progress, and completion. " +
                      "JWT tokens are issued by the Account Service (port 8081). " +
                      "Use the Authorize button to provide your Bearer token.",
        contact = @Contact(name = "RideLink Team - Member 3")
    ),
    servers = {
        @Server(url = "http://localhost:8083", description = "Local Development Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter the JWT token obtained from Account Service POST /api/auth/login. " +
                  "Format: Bearer <your-token>"
)
public class OpenApiConfig {
    // Configuration is handled via annotations above
}
