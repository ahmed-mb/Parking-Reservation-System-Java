package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.service.ParkingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authorization regression test for {@link ParkingController}.
 *
 * <p>Before the audit fix, a logged-in <em>Customer</em> could call
 * {@code POST /api/parking/initialize}, {@code POST /api/parking/reset},
 * and the spot create/update/delete endpoints — a privilege escalation.
 * These tests lock that down: only Admins may mutate parking state.
 *
 * <p><b>Why @SpringBootTest, not @WebMvcTest.</b> The privilege check is
 * implemented with {@code @PreAuthorize}, which only kicks in if Spring
 * Security's {@code @EnableMethodSecurity} infrastructure is fully wired.
 * The MVC slice ({@code @WebMvcTest}) loads the controller and its imports
 * but does not always activate the method-security advisor — so the
 * annotation is silently skipped and the endpoint returns 200 instead of
 * 403. Booting the full Spring context (matching the convention used by
 * the rest of this test suite) is the simplest reliable fix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ParkingService parkingService;

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
