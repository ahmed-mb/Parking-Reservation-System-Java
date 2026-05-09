package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.service.ParkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ParkingController.
 *
 * After the audit fix, every mutating endpoint plus the full-list / lookup
 * endpoints require the {@code Admin} authority. The public endpoints
 * remain {@code GET /api/parking/available} and
 * {@code GET /api/parking/available/count}. Errors are translated by
 * {@link com.ahmedbahaj.parking.exception.GlobalExceptionHandler}, so
 * specific exception types map to specific HTTP statuses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingService parkingService;

    private Parking testParking;

    @BeforeEach
    void setUp() {
        testParking = new Parking();
        testParking.setParkingId("A-001");
        testParking.setAvailability("available");
    }

    // ---------- public endpoints ----------------------------------------------

    @Test
    @DisplayName("GET /api/parking/available - public")
    void getAvailableParkingSpots_shouldReturnList() throws Exception {
        when(parkingService.getAvailableParkingSpots()).thenReturn(List.of(testParking));

        mockMvc.perform(get("/api/parking/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/parking/available/count - public")
    void getAvailableCount_shouldReturnCount() throws Exception {
        when(parkingService.getAvailableCount()).thenReturn(5L);

        mockMvc.perform(get("/api/parking/available/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(5));
    }

    // ---------- admin-only reads ----------------------------------------------

    @Test
    @DisplayName("GET /api/parking - Admin can list all spots")
    @WithMockUser(authorities = "Admin")
    void getAllParkingSpots_asAdmin_shouldReturnList() throws Exception {
        when(parkingService.getAllParkingSpots()).thenReturn(List.of(testParking));

        mockMvc.perform(get("/api/parking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/parking - Customer is forbidden")
    @WithMockUser(authorities = "Customer")
    void getAllParkingSpots_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/parking")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/parking/{id} - Admin lookup succeeds")
    @WithMockUser(authorities = "Admin")
    void getParkingSpotById_shouldReturnSpot() throws Exception {
        when(parkingService.getParkingSpotById("A-001")).thenReturn(testParking);

        mockMvc.perform(get("/api/parking/A-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingId").value("A-001"));
    }

    @Test
    @DisplayName("GET /api/parking/{id} - missing spot yields 404")
    @WithMockUser(authorities = "Admin")
    void getParkingSpotById_notFound_shouldReturn404() throws Exception {
        when(parkingService.getParkingSpotById("X-999"))
            .thenThrow(new ResourceNotFoundException("Parking spot not found"));

        mockMvc.perform(get("/api/parking/X-999"))
                .andExpect(status().isNotFound());
    }

    // ---------- admin-only mutations ------------------------------------------

    @Test
    @DisplayName("POST /api/parking - Admin can create a spot")
    @WithMockUser(authorities = "Admin")
    void createParkingSpot_asAdmin_shouldSucceed() throws Exception {
        when(parkingService.createParkingSpot("D-001")).thenReturn(testParking);

        mockMvc.perform(post("/api/parking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("parkingId", "D-001"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/parking - duplicate spot yields 400")
    @WithMockUser(authorities = "Admin")
    void createParkingSpot_duplicate_shouldReturnBadRequest() throws Exception {
        // ParkingService throws IllegalArgumentException for existsById; that
        // now maps cleanly to 400 via the new business-rule handler.
        when(parkingService.createParkingSpot("A-001"))
            .thenThrow(new IllegalArgumentException("Parking spot already exists"));

        mockMvc.perform(post("/api/parking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("parkingId", "A-001"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/parking - invalid id is rejected by validation")
    @WithMockUser(authorities = "Admin")
    void createParkingSpot_invalidPattern_shouldReturnBadRequest() throws Exception {
        // The new @Pattern("^[A-Z]-\\d{3}$") on the request body rejects this
        // payload before it reaches the service.
        mockMvc.perform(post("/api/parking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("parkingId", "not-a-spot"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/parking/{id} - Admin updates status")
    @WithMockUser(authorities = "Admin")
    void updateParkingStatus_asAdmin_shouldSucceed() throws Exception {
        when(parkingService.updateParkingStatus("A-001", "booked")).thenReturn(testParking);

        mockMvc.perform(put("/api/parking/A-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "booked"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/parking/{id} - missing spot yields 404")
    @WithMockUser(authorities = "Admin")
    void updateParkingStatus_missingSpot_shouldReturn404() throws Exception {
        when(parkingService.updateParkingStatus("X-999", "booked"))
            .thenThrow(new ResourceNotFoundException("Parking spot not found"));

        mockMvc.perform(put("/api/parking/X-999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "booked"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/parking/{id} - Admin deletes a spot")
    @WithMockUser(authorities = "Admin")
    void deleteParkingSpot_asAdmin_shouldSucceed() throws Exception {
        doNothing().when(parkingService).deleteParkingSpot("A-001");

        mockMvc.perform(delete("/api/parking/A-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking spot deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/parking/{id} - missing spot yields 404")
    @WithMockUser(authorities = "Admin")
    void deleteParkingSpot_missingSpot_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Parking spot not found"))
            .when(parkingService).deleteParkingSpot("X-999");

        mockMvc.perform(delete("/api/parking/X-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/parking/initialize - Admin only")
    @WithMockUser(authorities = "Admin")
    void initializeParkingSpots_asAdmin_shouldSucceed() throws Exception {
        doNothing().when(parkingService).initializeParkingSpots();

        mockMvc.perform(post("/api/parking/initialize"))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking spots initialized successfully"));
    }

    @Test
    @DisplayName("POST /api/parking/reset - Admin only")
    @WithMockUser(authorities = "Admin")
    void resetAllParkingSpots_asAdmin_shouldSucceed() throws Exception {
        doNothing().when(parkingService).resetAllParkingSpots();

        mockMvc.perform(post("/api/parking/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("All parking spots reset to available"));
    }

    // ---------- privilege-escalation regression -------------------------------

    @Test
    @DisplayName("Customer cannot reset parking (privilege escalation regression)")
    @WithMockUser(authorities = "Customer")
    void resetAllParkingSpots_asCustomer_shouldReturnForbidden() throws Exception {
        // Before the audit fix this returned 200 OK from a Customer token.
        mockMvc.perform(post("/api/parking/reset"))
                .andExpect(status().isForbidden());
    }
}
