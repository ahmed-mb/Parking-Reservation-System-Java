package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.service.ParkingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ahmedbahaj.parking.config.SecurityConfig;
import com.ahmedbahaj.parking.security.JwtAuthenticationFilter;
import com.ahmedbahaj.parking.security.JwtUtil;
import com.ahmedbahaj.parking.security.RateLimitingFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authorization regression test for {@link ParkingController}.
 *
 * Before the audit fix, a logged-in <em>Customer</em> could call
 * {@code POST /api/parking/initialize}, {@code POST /api/parking/reset},
 * and the spot create/update/delete endpoints — a privilege escalation.
 * These tests lock that down: only Admins may mutate parking state.
 */
@WebMvcTest(ParkingController.class)
@Import({SecurityConfig.class})
@ActiveProfiles("test")
class ParkingControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ParkingService parkingService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RateLimitingFilter rateLimitingFilter;

    @Test
    @WithMockUser(authorities = "Customer")
    @DisplayName("customers may NOT initialize parking spots")
    void customer_cannotInitialize() throws Exception {
        mockMvc.perform(post("/api/parking/initialize"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    @DisplayName("customers may NOT reset parking spots")
    void customer_cannotReset() throws Exception {
        mockMvc.perform(post("/api/parking/reset"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    @DisplayName("customers may NOT delete parking spots")
    void customer_cannotDelete() throws Exception {
        mockMvc.perform(delete("/api/parking/A-001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    @DisplayName("customers may NOT create parking spots")
    void customer_cannotCreate() throws Exception {
        mockMvc.perform(post("/api/parking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingId\":\"D-001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    @DisplayName("customers may NOT update parking status")
    void customer_cannotUpdateStatus() throws Exception {
        mockMvc.perform(put("/api/parking/A-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"available\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("public availability count remains accessible without auth")
    void anonymous_canReadAvailableCount() throws Exception {
        mockMvc.perform(get("/api/parking/available/count"))
                .andExpect(status().isOk());
    }
}
