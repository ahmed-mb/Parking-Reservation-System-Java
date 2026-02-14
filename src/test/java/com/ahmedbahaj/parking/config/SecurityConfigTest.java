package com.ahmedbahaj.parking.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    @DisplayName("passwordEncoder bean should be BCrypt")
    void passwordEncoder_shouldBeBCrypt() {
        String encoded = passwordEncoder.encode("test");
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$"));
        assertTrue(passwordEncoder.matches("test", encoded));
    }

    @Test
    @DisplayName("public endpoints should be accessible without auth")
    void publicEndpoints_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking/available/count"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("protected endpoints should require auth")
    void protectedEndpoints_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CORS configuration source should be configured")
    void corsConfigurationSource_shouldBeConfigured() {
        assertNotNull(corsConfigurationSource);
    }

    @Test
    @DisplayName("static resources endpoint is configured in security")
    void staticResources_shouldNotReturn401or403() throws Exception {
        // The "/" endpoint is configured as permitAll in SecurityConfig
        // In test context without actual static resources, it may return 500
        // This test verifies security doesn't block it (not 401/403)
        mockMvc.perform(get("/login"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Security allows access if status is not 401 or 403
                    assertTrue(status != 401 && status != 403, 
                        "Expected security to allow access, got status: " + status);
                });
    }
}
