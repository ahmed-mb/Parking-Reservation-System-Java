package com.ahmedbahaj.parking.controller;

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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Test
    @DisplayName("GET /api/parking - get all parking spots")
    @WithMockUser
    void getAllParkingSpots_shouldReturnList() throws Exception {
        when(parkingService.getAllParkingSpots()).thenReturn(List.of(testParking));

        mockMvc.perform(get("/api/parking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/parking/available - get available spots (public)")
    void getAvailableParkingSpots_shouldReturnList() throws Exception {
        when(parkingService.getAvailableParkingSpots()).thenReturn(List.of(testParking));

        mockMvc.perform(get("/api/parking/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/parking/available/count - get available count (public)")
    void getAvailableCount_shouldReturnCount() throws Exception {
        when(parkingService.getAvailableCount()).thenReturn(5L);

        mockMvc.perform(get("/api/parking/available/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(5));
    }

    @Test
    @DisplayName("GET /api/parking/{id} - get spot by id")
    @WithMockUser
    void getParkingSpotById_shouldReturnSpot() throws Exception {
        when(parkingService.getParkingSpotById("A-001")).thenReturn(testParking);

        mockMvc.perform(get("/api/parking/A-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingId").value("A-001"));
    }

    @Test
    @DisplayName("GET /api/parking/{id} - not found")
    @WithMockUser
    void getParkingSpotById_notFound_shouldReturnBadRequest() throws Exception {
        when(parkingService.getParkingSpotById("X-999"))
            .thenThrow(new RuntimeException("Parking spot not found"));

        mockMvc.perform(get("/api/parking/X-999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/parking - create spot")
    @WithMockUser
    void createParkingSpot_shouldReturnCreatedSpot() throws Exception {
        when(parkingService.createParkingSpot("D-001")).thenReturn(testParking);

        mockMvc.perform(post("/api/parking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("parkingId", "D-001"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/parking - create fails")
    @WithMockUser
    void createParkingSpot_fails_shouldReturnBadRequest() throws Exception {
        when(parkingService.createParkingSpot("A-001"))
            .thenThrow(new RuntimeException("Spot already exists"));

        mockMvc.perform(post("/api/parking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("parkingId", "A-001"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/parking/{id} - update status")
    @WithMockUser
    void updateParkingStatus_shouldReturnUpdatedSpot() throws Exception {
        when(parkingService.updateParkingStatus("A-001", "booked")).thenReturn(testParking);

        mockMvc.perform(put("/api/parking/A-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "booked"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/parking/{id} - update fails")
    @WithMockUser
    void updateParkingStatus_fails_shouldReturnBadRequest() throws Exception {
        when(parkingService.updateParkingStatus("X-999", "booked"))
            .thenThrow(new RuntimeException("Spot not found"));

        mockMvc.perform(put("/api/parking/X-999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "booked"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/parking/{id} - delete spot")
    @WithMockUser
    void deleteParkingSpot_shouldReturnSuccess() throws Exception {
        doNothing().when(parkingService).deleteParkingSpot("A-001");

        mockMvc.perform(delete("/api/parking/A-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking spot deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/parking/{id} - delete fails")
    @WithMockUser
    void deleteParkingSpot_fails_shouldReturnBadRequest() throws Exception {
        doThrow(new RuntimeException("Cannot delete")).when(parkingService).deleteParkingSpot("X-999");

        mockMvc.perform(delete("/api/parking/X-999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/parking/initialize - initialize spots")
    @WithMockUser
    void initializeParkingSpots_shouldReturnSuccess() throws Exception {
        doNothing().when(parkingService).initializeParkingSpots();

        mockMvc.perform(post("/api/parking/initialize"))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking spots initialized successfully"));
    }

    @Test
    @DisplayName("POST /api/parking/reset - reset all spots")
    @WithMockUser
    void resetAllParkingSpots_shouldReturnSuccess() throws Exception {
        doNothing().when(parkingService).resetAllParkingSpots();

        mockMvc.perform(post("/api/parking/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("All parking spots reset to available"));
    }
}
