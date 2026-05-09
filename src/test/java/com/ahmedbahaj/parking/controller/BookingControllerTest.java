package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.dto.BookingRequest;
import com.ahmedbahaj.parking.exception.InsufficientCreditException;
import com.ahmedbahaj.parking.exception.ParkingNotAvailableException;
import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.service.BookingService;
import com.ahmedbahaj.parking.service.UserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private UserService userService;

    private User testUser;
    private User adminUser;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("Customer");

        adminUser = new User();
        adminUser.setId(2);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("Admin");

        testBooking = new Booking();
        testBooking.setId(1);
        testBooking.setUserId(1);
        testBooking.setUserName("testuser");
        testBooking.setParkingSpot("A-001");
        testBooking.setStatus("Active");
        testBooking.setDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/bookings - admin only")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void getAllBookings_asAdmin_shouldReturnList() throws Exception {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings - forbidden for customer")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getAllBookings_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/bookings - create own booking")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void createBooking_ownBooking_shouldSucceed() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(1);
        request.setParkingId("A-001");
        request.setCarPlate("ABC123");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingService.createBooking(1, "A-001", "ABC123")).thenReturn(testBooking);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/bookings - cannot create for another user")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void createBooking_forOtherUser_shouldReturnForbidden() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(999);
        request.setParkingId("A-001");
        request.setCarPlate("ABC123");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/bookings - admin can create for anyone")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void createBooking_asAdmin_canCreateForAnyone() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(1);
        request.setParkingId("A-001");
        request.setCarPlate("ABC123");

        when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);
        when(bookingService.createBooking(1, "A-001", "ABC123")).thenReturn(testBooking);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/bookings - insufficient credit yields 400")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void createBooking_withInsufficientCredit_shouldReturnBadRequest() throws Exception {
        // After the audit fix, controllers no longer wrap exceptions in a
        // generic 400. Specific business exceptions are mapped by
        // GlobalExceptionHandler — InsufficientCreditException -> 400.
        BookingRequest request = new BookingRequest();
        request.setUserId(1);
        request.setParkingId("A-001");
        request.setCarPlate("ABC123");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingService.createBooking(1, "A-001", "ABC123"))
            .thenThrow(new InsufficientCreditException("Insufficient credit"));

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel - cancel own booking")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void cancelBooking_ownBooking_shouldSucceed() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        doNothing().when(bookingService).cancelBooking(1);

        mockMvc.perform(post("/api/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking cancelled successfully"));
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel - cannot cancel other's booking")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void cancelBooking_otherBooking_shouldReturnForbidden() throws Exception {
        Booking otherBooking = new Booking();
        otherBooking.setId(2);
        otherBooking.setUserId(999);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(2)).thenReturn(Optional.of(otherBooking));

        mockMvc.perform(post("/api/bookings/2/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel - booking not found yields 404")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void cancelBooking_notFound_shouldReturn404() throws Exception {
        // Controller now throws ResourceNotFoundException, which the
        // GlobalExceptionHandler maps to 404 (was incorrectly 400 before).
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/bookings/999/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel - admin can cancel any")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void cancelBooking_asAdmin_canCancelAny() throws Exception {
        when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        doNothing().when(bookingService).cancelBooking(1);

        mockMvc.perform(post("/api/bookings/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/cancel - business-rule failure yields 400")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void cancelBooking_alreadyCancelled_shouldReturnBadRequest() throws Exception {
        // IllegalStateException now maps cleanly to 400 via GlobalExceptionHandler.
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        doThrow(new IllegalStateException("Booking is already cancelled or completed"))
            .when(bookingService).cancelBooking(1);

        mockMvc.perform(post("/api/bookings/1/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/bookings/user/{userId} - get own bookings")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getUserBookings_ownBookings_shouldSucceed() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findByUserId(1)).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/bookings/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/user/{userId} - cannot get other's bookings")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getUserBookings_otherBookings_shouldReturnForbidden() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        mockMvc.perform(get("/api/bookings/user/999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/bookings/user/{userId} - missing user yields 404")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getUserBookings_missingUser_shouldReturnNotFound() throws Exception {
        // Resource lookup failures now travel as ResourceNotFoundException -> 404
        // instead of being squashed into a 400 with an internal message.
        when(userService.getUserByEmail("test@example.com"))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/bookings/user/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/bookings/active - admin only")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void getActiveBookings_asAdmin_shouldSucceed() throws Exception {
        when(bookingRepository.findByStatus("Active")).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/bookings/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/active - forbidden for customer")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getActiveBookings_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/bookings/active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/bookings/my-active - get current user's active booking")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getMyActiveBooking_shouldReturnBookings() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findByUserIdAndStatus(1, "Active")).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/bookings/my-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/my-active - missing user yields 404")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getMyActiveBooking_missingUser_shouldReturnNotFound() throws Exception {
        when(userService.getUserByEmail("test@example.com"))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/bookings/my-active"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/report-taken - report own booking spot")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void reportSpotTaken_ownBooking_shouldSucceed() throws Exception {
        Booking reassignedBooking = new Booking();
        reassignedBooking.setId(1);
        reassignedBooking.setParkingSpot("B-001");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(bookingService.reportSpotTaken(1)).thenReturn(reassignedBooking);

        mockMvc.perform(post("/api/bookings/1/report-taken"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/report-taken - cannot report other's booking")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void reportSpotTaken_otherBooking_shouldReturnForbidden() throws Exception {
        Booking otherBooking = new Booking();
        otherBooking.setId(2);
        otherBooking.setUserId(999);

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(2)).thenReturn(Optional.of(otherBooking));

        mockMvc.perform(post("/api/bookings/2/report-taken"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/report-taken - no spots yields 409")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void reportSpotTaken_noSpotsAvailable_shouldReturnConflict() throws Exception {
        // ParkingNotAvailableException maps to 409 Conflict in
        // GlobalExceptionHandler — semantically correct for an out-of-resources
        // condition.
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(bookingService.reportSpotTaken(1))
            .thenThrow(new ParkingNotAvailableException("No available parking spots"));

        mockMvc.perform(post("/api/bookings/1/report-taken"))
                .andExpect(status().isConflict());
    }
}
