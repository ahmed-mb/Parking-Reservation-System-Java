package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.dto.LoginRequest;
import com.ahmedbahaj.parking.dto.LoginResponse;
import com.ahmedbahaj.parking.dto.RegisterRequest;
import com.ahmedbahaj.parking.dto.UpdateUserRequest;
import com.ahmedbahaj.parking.exception.DuplicateEmailException;
import com.ahmedbahaj.parking.exception.InsufficientCreditException;
import com.ahmedbahaj.parking.exception.InvalidCredentialsException;
import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RecaptchaService recaptchaService;

    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setMobile("1234567890");
        testUser.setAddress("123 Test St");
        testUser.setCarPlateNo("ABC123");
        testUser.setCredit(new BigDecimal("10.00"));
        testUser.setRole("Customer");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password@123");
        loginRequest.setRecaptchaToken("valid-token");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setMobile("0987654321");
        registerRequest.setAddress("456 New St");
        registerRequest.setCarPlateNo("XYZ789");
        registerRequest.setRecaptchaToken("valid-token");
    }

    @Test
    @DisplayName("login should return token on valid credentials")
    void login_withValidCredentials_shouldReturnToken() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", "Customer", 0)).thenReturn("jwt-token");

        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("testuser", response.getUsername());
        assertEquals("Customer", response.getRole());
    }

    @Test
    @DisplayName("login should throw exception when reCAPTCHA fails")
    void login_whenRecaptchaFails_shouldThrowException() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
            userService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("login should throw exception when user not found")
    void login_whenUserNotFound_shouldThrowException() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
            userService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("login should throw exception when password incorrect")
    void login_whenPasswordIncorrect_shouldThrowException() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
            userService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("register should create new user")
    void register_withValidData_shouldCreateUser() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        doNothing().when(passwordValidator).validate(anyString(), anyString());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(2);
            return u;
        });

        User result = userService.register(registerRequest);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("Customer", result.getRole());
        assertEquals(BigDecimal.ZERO, result.getCredit());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register should throw exception when reCAPTCHA fails")
    void register_whenRecaptchaFails_shouldThrowException() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
            userService.register(registerRequest)
        );
    }

    @Test
    @DisplayName("register should throw exception when email exists")
    void register_whenEmailExists_shouldThrowException() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        doNothing().when(passwordValidator).validate(anyString(), anyString());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(DuplicateEmailException.class, () ->
            userService.register(registerRequest)
        );
    }

    @Test
    @DisplayName("register should throw exception when password invalid")
    void register_whenPasswordInvalid_shouldThrowException() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        doThrow(new IllegalArgumentException("Password too short"))
            .when(passwordValidator).validate(anyString(), anyString());

        assertThrows(IllegalArgumentException.class, () ->
            userService.register(registerRequest)
        );
    }

    @Test
    @DisplayName("getUserByEmail should return user when exists")
    void getUserByEmail_whenExists_shouldReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    @DisplayName("getUserByEmail should throw exception when not exists")
    void getUserByEmail_whenNotExists_shouldThrowException() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            userService.getUserByEmail("notfound@example.com")
        );
    }

    @Test
    @DisplayName("updateUser should update user fields")
    void updateUser_shouldUpdateFields() {
        UpdateUserRequest updatedUser = new UpdateUserRequest();
        updatedUser.setUsername("updatedname");
        updatedUser.setEmail("test@example.com"); // same email
        updatedUser.setMobile("9999999999");
        updatedUser.setAddress("999 Updated St");
        updatedUser.setCarPlateNo("UPD999");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1, updatedUser);

        assertEquals("updatedname", result.getUsername());
        assertEquals("9999999999", result.getMobile());
        assertEquals("999 Updated St", result.getAddress());
        assertEquals("UPD999", result.getCarPlateNo());
    }

    @Test
    @DisplayName("updateUser should throw exception when changing to existing email")
    void updateUser_whenEmailExists_shouldThrowException() {
        UpdateUserRequest updatedUser = new UpdateUserRequest();
        updatedUser.setEmail("existing@example.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateEmailException.class, () ->
            userService.updateUser(1, updatedUser)
        );
    }

    @Test
    @DisplayName("addCredit should increase user credit")
    void addCredit_shouldIncreaseCredit() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.addCredit(1, new BigDecimal("5.00"));

        assertEquals(new BigDecimal("15.00"), testUser.getCredit());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deductCredit should decrease user credit")
    void deductCredit_shouldDecreaseCredit() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.deductCredit(1, new BigDecimal("5.00"));

        assertEquals(new BigDecimal("5.00"), testUser.getCredit());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deductCredit should throw exception when insufficient credit")
    void deductCredit_whenInsufficientCredit_shouldThrowException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        assertThrows(InsufficientCreditException.class, () ->
            userService.deductCredit(1, new BigDecimal("15.00"))
        );
    }

    @Test
    @DisplayName("updateUser should update credit when provided")
    void updateUser_shouldUpdateCredit() {
        UpdateUserRequest updatedUser = new UpdateUserRequest();
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("test@example.com");
        updatedUser.setMobile("1234567890");
        updatedUser.setAddress("123 Test St");
        updatedUser.setCarPlateNo("ABC123");
        updatedUser.setCredit(new BigDecimal("50.00"));

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1, updatedUser);

        assertEquals(new BigDecimal("50.00"), result.getCredit());
    }

    @Test
    @DisplayName("updateUser should bump tokenVersion when role actually changes")
    void updateUser_whenRoleChanges_shouldBumpTokenVersion() {
        UpdateUserRequest updatedUser = new UpdateUserRequest();
        updatedUser.setRole("Admin");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1, updatedUser);

        assertEquals("Admin", result.getRole());
        assertEquals(1, result.getTokenVersion());
    }

    @Test
    @DisplayName("updateUser should not bump tokenVersion when role is unchanged")
    void updateUser_whenRoleUnchanged_shouldNotBumpTokenVersion() {
        UpdateUserRequest updatedUser = new UpdateUserRequest();
        updatedUser.setRole("Customer"); // same as testUser's current role

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1, updatedUser);

        assertEquals("Customer", result.getRole());
    }

    @Test
    @DisplayName("updateUser should leave role and credit untouched when the request never mentions them")
    void updateUser_omittingRoleAndCredit_shouldNotResetThem() {
        // Regression test: the admin panel's edit-customer form never sends
        // "role" or "credit" at all. Binding the raw User entity as the
        // request body used to mean Jackson defaulted those two fields to
        // "Customer"/0.00 (the entity's own field initializers) instead of
        // leaving them null, silently wiping an Admin's role and any
        // accumulated credit on every unrelated profile edit.
        User targetAdmin = new User();
        targetAdmin.setId(1);
        targetAdmin.setUsername("adminuser");
        targetAdmin.setEmail("admin2@example.com");
        targetAdmin.setRole("Admin");
        targetAdmin.setCredit(new BigDecimal("42.00"));

        UpdateUserRequest updatedUser = new UpdateUserRequest();
        updatedUser.setMobile("5551234567"); // the only field this "request" carries

        when(userRepository.findById(1)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.updateUser(1, updatedUser);

        assertEquals("5551234567", result.getMobile());
        assertEquals("Admin", result.getRole());
        assertEquals(new BigDecimal("42.00"), result.getCredit());
        assertEquals(0, result.getTokenVersion()); // unchanged since role never changed
        assertEquals(0, result.getTokenVersion());
    }

    @Test
    @DisplayName("login should lock account after 5 failed attempts")
    void login_shouldLockAccountAfterFiveFailedAttempts() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        when(userRepository.findByEmail("locktest@example.com")).thenReturn(Optional.empty());

        // Make 5 failed attempts
        for (int i = 0; i < 5; i++) {
            loginRequest.setEmail("locktest@example.com");
            assertThrows(InvalidCredentialsException.class, () ->
                userService.login(loginRequest)
            );
        }

        // 6th attempt should be locked
        loginRequest.setEmail("locktest@example.com");
        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> userService.login(loginRequest)
        );
        assertTrue(exception.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("login should record failed attempt on wrong password")
    void login_wrongPassword_shouldRecordFailedAttempt() {
        when(recaptchaService.verifyRecaptcha(anyString())).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password@123", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
            userService.login(loginRequest)
        );
    }
}
