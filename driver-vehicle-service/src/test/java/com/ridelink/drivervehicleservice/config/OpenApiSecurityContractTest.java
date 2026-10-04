package com.ridelink.drivervehicleservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies OpenAPI declares bearerAuth on POST /api/drivers so Swagger UI
 * attaches Authorization: Bearer &lt;JWT&gt; after Authorize.
 */
@SpringBootTest(properties = {
        "jwt.secret=test_secret_key_for_unit_tests_only_not_real_value_32b",
        "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_driver_vehicle_db_test",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.config.import=optional:file:nonexistent.env[.properties]"
})
@AutoConfigureMockMvc
class OpenApiSecurityContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postDriversDeclaresBearerAuthSecurity() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(root.path("components").path("securitySchemes").has("bearerAuth"),
                "bearerAuth security scheme must be registered");
        JsonNode bearer = root.path("components").path("securitySchemes").path("bearerAuth");
        assertEquals("http", bearer.path("type").asText());
        assertEquals("bearer", bearer.path("scheme").asText());

        JsonNode postDrivers = root.path("paths").path("/api/drivers").path("post");
        assertFalse(postDrivers.isMissingNode(), "POST /api/drivers must exist in OpenAPI");

        JsonNode security = postDrivers.path("security");
        if (security.isMissingNode() || security.isEmpty()) {
            security = root.path("security");
        }
        assertFalse(security.isMissingNode() || security.isEmpty(),
                "POST /api/drivers must inherit or declare bearerAuth security");

        boolean hasBearer = false;
        for (JsonNode req : security) {
            if (req.has("bearerAuth")) {
                hasBearer = true;
                break;
            }
        }
        assertTrue(hasBearer, "POST /api/drivers OpenAPI security must include bearerAuth");
    }
}
