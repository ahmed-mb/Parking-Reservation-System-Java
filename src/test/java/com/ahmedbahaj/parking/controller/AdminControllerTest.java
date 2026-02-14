package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.repository.ParkingRepository;
import com.ahmedbahaj.parking.repository.UserRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private ParkingRepository parkingRepository;

    @MockBean
    private UserService userService;

    private User testUser;
    private Booking testBooking;
    private Parking testParking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("Customer");
        testUser.setCredit(new BigDecimal("10.00"));

        testBooking = new Booking();
        testBooking.setId(1);
        testBooking.setUserId(1);
        testBooking.setUserName("testuser");
        testBooking.setParkingSpot("A-001");
        testBooking.setStatus("Active");

        testParking = new Parking();
        testParking.setParkingId("A-001");
        testParking.setAvailability("booked");
    }

    @Test
    @DisplayName("GET /api/admin/users - get all users")
    @WithMockUser(authorities = {"Admin"})
    void getAllUsers_shouldReturnList() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/users - forbidden for non-admin")
    @WithMockUser(authorities = {"Customer"})
    void getAllUsers_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - get user by id")
    @WithMockUser(authorities = {"Admin"})
    void getUserById_shouldReturnUser() throws Exception {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - user not found")
    @WithMockUser(authorities = {"Admin"})
    void getUserById_notFound_shouldReturn404() throws Exception {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id} - update user")
    @WithMockUser(authorities = {"Admin"})
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        when(userService.updateUser(eq(1), any(User.class))).thenReturn(testUser);

        mockMvc.perform(put("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id} - update fails")
    @WithMockUser(authorities = {"Admin"})
    void updateUser_fails_shouldReturnBadRequest() throws Exception {
        when(userService.updateUser(eq(1), any(User.class)))
            .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} - delete user")
    @WithMockUser(authorities = {"Admin"})
    void deleteUser_shouldReturnSuccess() throws Exception {
        doNothing().when(userRepository).deleteById(1);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} - delete fails")
    @WithMockUser(authorities = {"Admin"})
    void deleteUser_fails_shouldReturnBadRequest() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(userRepository).deleteById(999);

        mockMvc.perform(delete("/api/admin/users/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/credit - add credit")
    @WithMockUser(authorities = {"Admin"})
    void addCreditToUser_shouldReturnSuccess() throws Exception {
        doNothing().when(userService).addCredit(eq(1), any(BigDecimal.class));

        mockMvc.perform(post("/api/admin/users/1/credit")
                .param("amount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit added successfully"));
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/credit - add credit fails")
    @WithMockUser(authorities = {"Admin"})
    void addCreditToUser_fails_shouldReturnBadRequest() throws Exception {
        doThrow(new RuntimeException("User not found")).when(userService).addCredit(eq(999), any(BigDecimal.class));

        mockMvc.perform(post("/api/admin/users/999/credit")
                .param("amount", "10.00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/admin/bookings - get all bookings")
    @WithMockUser(authorities = {"Admin"})
    void getAllBookings_shouldReturnList() throws Exception {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/bookings/active - get active bookings")
    @WithMockUser(authorities = {"Admin"})
    void getActiveBookings_shouldReturnList() throws Exception {
        when(bookingRepository.findByStatus("Active")).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/admin/bookings/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/bookings/user/{userId} - get bookings by user")
    @WithMockUser(authorities = {"Admin"})
    void getBookingsByUserId_shouldReturnList() throws Exception {
        when(bookingRepository.findByUserId(1)).thenReturn(List.of(testBooking));

        mockMvc.perform(get("/api/admin/bookings/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - delete booking with refund")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_activeWithUser_shouldRefundAndRelease() throws Exception {
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Booking deleted successfully"));

        verify(userRepository).save(any(User.class));
        verify(parkingRepository).save(any(Parking.class));
        verify(bookingRepository).deleteById(1);
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - delete completed booking")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_completed_shouldNotRefund() throws Exception {
        testBooking.setStatus("Completed");
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - delete booking with Unknown user")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_unknownUser_shouldNotRefund() throws Exception {
        testBooking.setUserName("Unknown");
        testBooking.setUserId(0);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - booking not found")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_notFound_shouldReturnBadRequest() throws Exception {
        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/admin/bookings/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - release unknown parking spot")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_unknownSpot_shouldRelease() throws Exception {
        testParking.setAvailability("unknown");
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk());

        verify(parkingRepository).save(any(Parking.class));
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - null parking spot")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_nullParkingSpot_shouldSucceed() throws Exception {
        testBooking.setParkingSpot(null);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/admin/bookings/{id} - null status treated as active")
    @WithMockUser(authorities = {"Admin"})
    void deleteBooking_nullStatus_shouldRefund() throws Exception {
        testBooking.setStatus(null);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("GET /api/admin/parking - get all parking spots")
    @WithMockUser(authorities = {"Admin"})
    void getAllParkingSpots_shouldReturnList() throws Exception {
        when(parkingRepository.findAll()).thenReturn(List.of(testParking));

        mockMvc.perform(get("/api/admin/parking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/parking/stats - get parking stats")
    @WithMockUser(authorities = {"Admin"})
    void getParkingStats_shouldReturnStats() throws Exception {
        when(parkingRepository.count()).thenReturn(10L);
        when(parkingRepository.countByAvailability("available")).thenReturn(5L);
        when(parkingRepository.countByAvailability("booked")).thenReturn(4L);
        when(parkingRepository.countByAvailability("unknown")).thenReturn(1L);

        mockMvc.perform(get("/api/admin/parking/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.available").value(5))
                .andExpect(jsonPath("$.booked").value(4))
                .andExpect(jsonPath("$.unknown").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - get dashboard stats")
    @WithMockUser(authorities = {"Admin"})
    void getDashboardStats_shouldReturnStats() throws Exception {
        when(userRepository.count()).thenReturn(50L);
        when(bookingRepository.count()).thenReturn(100L);
        when(bookingRepository.findByStatus("Active")).thenReturn(List.of(testBooking));
        when(parkingRepository.count()).thenReturn(10L);
        when(parkingRepository.countByAvailability("available")).thenReturn(5L);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(50))
                .andExpect(jsonPath("$.totalBookings").value(100))
                .andExpect(jsonPath("$.activeBookings").value(1))
                .andExpect(jsonPath("$.totalParkingSpots").value(10))
                .andExpect(jsonPath("$.availableSpots").value(5));
    }
}
