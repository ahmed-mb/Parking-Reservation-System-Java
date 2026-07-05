package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.dto.LoginRequest;
import com.ahmedbahaj.parking.dto.LoginResponse;
import com.ahmedbahaj.parking.dto.RegisterRequest;
import com.ahmedbahaj.parking.dto.UpdateUserRequest;
import com.ahmedbahaj.parking.exception.DuplicateEmailException;
import com.ahmedbahaj.parking.exception.InvalidCredentialsException;
import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.User;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("Customer");
        testUser.setCredit(new BigDecimal("10.00"));

        adminUser = new User();
        adminUser.setId(2);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("encodedPassword");
        adminUser.setRole("Admin");
        adminUser.setCredit(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("POST /api/users/login - success")
    void login_withValidCredentials_shouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password@123");
        request.setRecaptchaToken("valid-token");

        LoginResponse response = new LoginResponse("jwt-token", "test@example.com", "testuser", "Customer");
        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/users/login - invalid credentials")
    void login_withInvalidCredentials_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");
        request.setRecaptchaToken("valid-token");

        when(userService.login(any(LoginRequest.class)))
            .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/users/register - success")
    void register_withValidData_shouldReturnUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("Password@123");
        request.setMobile("1234567890");
        request.setAddress("123 Test St");
        request.setCarPlateNo("ABC123");
        request.setRecaptchaToken("valid-token");

        when(userService.register(any(RegisterRequest.class))).thenReturn(testUser);

        // After the audit fix, /api/users/register returns 201 Created and a
        // UserResponse DTO (no password hash, no other internal columns).
        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/users/register - duplicate email")
    void register_withDuplicateEmail_shouldReturnBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("Password@123");
        request.setRecaptchaToken("valid-token");

        // DuplicateEmailException maps to 409 Conflict, not a generic 400.
        when(userService.register(any(RegisterRequest.class)))
            .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/users/me - authenticated user")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getCurrentUser_authenticated_shouldReturnUser() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/users/me - missing user yields 404")
    @WithMockUser(username = "notfound@example.com", authorities = {"Customer"})
    void getCurrentUser_notFound_shouldReturnNotFound() throws Exception {
        when(userService.getUserByEmail("notfound@example.com"))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/users - admin only")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void getAllUsers_asAdmin_shouldReturnUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(testUser, adminUser));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/users - forbidden for non-admin")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void getAllUsers_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - user updates own profile")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void updateUser_ownProfile_shouldSucceed() throws Exception {
        User updatedUser = new User();
        updatedUser.setUsername("updatedname");
        updatedUser.setEmail("test@example.com");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(userService.updateUser(eq(1), any(UpdateUserRequest.class))).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - forbidden to update other's profile")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void updateUser_otherProfile_shouldReturnForbidden() throws Exception {
        User updatedUser = new User();
        updatedUser.setUsername("hacker");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);

        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - admin can update any profile")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void updateUser_asAdmin_shouldSucceed() throws Exception {
        User updatedUser = new User();
        updatedUser.setUsername("updated");

        when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);
        when(userService.updateUser(eq(1), any(UpdateUserRequest.class))).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - missing target user yields 404")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void updateUser_missingUser_shouldReturnNotFound() throws Exception {
        User updatedUser = new User();
        updatedUser.setUsername("updated");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(userService.updateUser(eq(1), any(UpdateUserRequest.class)))
            .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users/{id}/credit - admin adds credit")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void addCredit_asAdmin_shouldSucceed() throws Exception {
        doNothing().when(userService).addCredit(eq(1), any(BigDecimal.class));

        mockMvc.perform(post("/api/users/1/credit")
                .param("amount", "10.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit added successfully"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/credit - negative amount rejected (JSON envelope)")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void addCredit_negativeAmount_shouldReturnBadRequest() throws Exception {
        // Validation failure now flows through GlobalExceptionHandler and
        // returns a structured ErrorResponse JSON, not a raw string body.
        mockMvc.perform(post("/api/users/1/credit")
                .param("amount", "-10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credit amount must be positive"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/credit - zero amount rejected (JSON envelope)")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void addCredit_zeroAmount_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/1/credit")
                .param("amount", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credit amount must be positive"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/credit - missing user yields 404")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void addCredit_missingUser_shouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
            .when(userService).addCredit(eq(999), any(BigDecimal.class));

        mockMvc.perform(post("/api/users/999/credit")
                .param("amount", "10.00"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users/{id}/credit - forbidden for non-admin")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void addCredit_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/users/1/credit")
                .param("amount", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - admin deletes user")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void deleteUser_asAdmin_shouldSucceed() throws Exception {
        when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);
        doNothing().when(userRepository).deleteById(1);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - admin cannot delete self (JSON envelope)")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void deleteUser_self_shouldReturnBadRequest() throws Exception {
        when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);

        mockMvc.perform(delete("/api/users/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete your own account"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - downstream failure yields 500 with generic body")
    @WithMockUser(username = "admin@example.com", authorities = {"Admin"})
    void deleteUser_withError_shouldReturnInternalServerError() throws Exception {
        // Unhandled RuntimeException now returns a generic 500 with no
        // internal message — preventing leakage of repository / vendor errors.
        when(userService.getUserByEmail("admin@example.com")).thenReturn(adminUser);
        doThrow(new RuntimeException("Delete failed")).when(userRepository).deleteById(999);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - forbidden for non-admin")
    @WithMockUser(username = "test@example.com", authorities = {"Customer"})
    void deleteUser_asCustomer_shouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());
    }
}
