package com.ahmedbahaj.parking.config;

import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseInitializerTest {

    @Mock
    private ParkingService parkingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DatabaseInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DatabaseInitializer(parkingService, userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "adminDefaultPassword", "admin");
    }

    @Test
    @DisplayName("run - should initialize parking and create admin if not exists")
    void run_shouldInitializeParkingAndCreateAdmin() {
        when(userRepository.findByEmail("admin@parking.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        initializer.run();

        verify(parkingService).initializeParkingSpots();
        verify(userRepository).findByEmail("admin@parking.com");
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertEquals("Admin", savedUser.getUsername());
        assertEquals("admin@parking.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals("Admin", savedUser.getRole());
    }

    @Test
    @DisplayName("run - should not create admin if already exists")
    void run_shouldNotCreateAdminIfExists() {
        User existingAdmin = new User();
        existingAdmin.setEmail("admin@parking.com");
        when(userRepository.findByEmail("admin@parking.com")).thenReturn(Optional.of(existingAdmin));

        initializer.run();

        verify(parkingService).initializeParkingSpots();
        verify(userRepository).findByEmail("admin@parking.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("run - should use custom password when not default")
    void run_shouldUseCustomPassword() {
        ReflectionTestUtils.setField(initializer, "adminDefaultPassword", "SecurePass@123");
        
        when(userRepository.findByEmail("admin@parking.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass@123")).thenReturn("encoded-secure-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        initializer.run();

        verify(passwordEncoder).encode("SecurePass@123");
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded-secure-password", userCaptor.getValue().getPassword());
    }
}
