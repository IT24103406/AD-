package com.ridelink.drivervehicleservice.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI / Swagger configuration.
 *
 * Global {@link SecurityRequirement} ensures Swagger UI attaches
 * {@code Authorization: Bearer <JWT>} to protected endpoints after Authorize.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "RideLink - Driver and Vehicle Service",
        version = "1.0.0",
        description = "Driver and vehicle management API. " +
                      "Obtain a JWT from Account Service (POST http://localhost:8081/api/auth/login), " +
                      "then use Authorize and paste the token (without the Bearer prefix)."
    ),
    security = {
        @SecurityRequirement(name = "bearerAuth")
    },
    servers = {
        @Server(url = "http://localhost:8082", description = "Local Development Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT issued by Account Service. Paste the raw token only; Swagger adds the Bearer prefix."
)
public class OpenApiConfig {
}
