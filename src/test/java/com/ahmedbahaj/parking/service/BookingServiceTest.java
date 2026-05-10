package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.exception.InsufficientCreditException;
import com.ahmedbahaj.parking.exception.ParkingNotAvailableException;
import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.repository.ParkingRepository;
import com.ahmedbahaj.parking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParkingRepository parkingRepository;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Parking testParking;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setMobile("1234567890");
        testUser.setCredit(new BigDecimal("10.00"));

        testParking = new Parking();
        testParking.setParkingId("A-001");
        testParking.setAvailability("available");

        testBooking = new Booking();
        testBooking.setId(1);
        testBooking.setUserId(1);
        testBooking.setUserName("testuser");
        testBooking.setParkingSpot("A-001");
        testBooking.setStatus("Active");
    }

    @Test
    @DisplayName("createBooking should create booking successfully")
    void createBooking_withSufficientCredit_shouldSucceed() {
        // Service uses findByIdForUpdate (pessimistic-write lock) to prevent
        // double-booking. The mock has to match.
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findByIdForUpdate("A-001")).thenReturn(Optional.of(testParking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(1);
            return b;
        });

        Booking result = bookingService.createBooking(1, "A-001", "ABC123");

        assertNotNull(result);
        assertEquals("A-001", result.getParkingSpot());
        assertEquals("Active", result.getStatus());
        assertEquals(new BigDecimal("4.00"), testUser.getCredit()); // 10 - 6
        assertEquals("booked", testParking.getAvailability());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking should throw exception when insufficient credit")
    void createBooking_withInsufficientCredit_shouldThrowException() {
        testUser.setCredit(new BigDecimal("5.00")); // Less than 6
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        assertThrows(InsufficientCreditException.class, () ->
            bookingService.createBooking(1, "A-001", "ABC123")
        );
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking should throw exception when parking not available")
    void createBooking_whenParkingBooked_shouldThrowException() {
        testParking.setAvailability("booked");
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findByIdForUpdate("A-001")).thenReturn(Optional.of(testParking));

        assertThrows(ParkingNotAvailableException.class, () ->
            bookingService.createBooking(1, "A-001", "ABC123")
        );
    }

    @Test
    @DisplayName("createBooking should throw exception when user not found")
    void createBooking_whenUserNotFound_shouldThrowException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            bookingService.createBooking(999, "A-001", "ABC123")
        );
    }

    @Test
    @DisplayName("createBooking should throw exception when parking not found")
    void createBooking_whenParkingNotFound_shouldThrowException() {
        // Mockito's strict stubbing flagged the pre-fix variant as unused
        // because the service no longer calls findById here.
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findByIdForUpdate("X-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            bookingService.createBooking(1, "X-999", "ABC123")
        );
    }

    @Test
    @DisplayName("cancelBooking should cancel and refund successfully")
    void cancelBooking_shouldCancelAndRefund() {
        testUser.setCredit(new BigDecimal("4.00")); // After previous booking
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));

        bookingService.cancelBooking(1);

        assertEquals("Cancelled", testBooking.getStatus());
        assertNotNull(testBooking.getCancelledDate());
        assertEquals(new BigDecimal("10.00"), testUser.getCredit()); // 4 + 6 refund
        assertEquals("available", testParking.getAvailability());
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("cancelBooking should throw exception when already cancelled")
    void cancelBooking_whenAlreadyCancelled_shouldThrowException() {
        testBooking.setStatus("Cancelled");
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        assertThrows(IllegalStateException.class, () ->
            bookingService.cancelBooking(1)
        );
    }

    @Test
    @DisplayName("cancelBooking should throw exception when booking not found")
    void cancelBooking_whenNotFound_shouldThrowException() {
        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            bookingService.cancelBooking(999)
        );
    }

    @Test
    @DisplayName("reportSpotTaken should reassign to new spot")
    void reportSpotTaken_shouldReassignToNewSpot() {
        Parking newParking = new Parking();
        newParking.setParkingId("B-001");
        newParking.setAvailability("available");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(parkingRepository.findByAvailability("available"))
            .thenReturn(Collections.singletonList(newParking));
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));
        when(parkingRepository.findById("B-001")).thenReturn(Optional.of(newParking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.reportSpotTaken(1);

        assertEquals("B-001", result.getParkingSpot());
        assertEquals("unknown", testParking.getAvailability());
        assertEquals("booked", newParking.getAvailability());
        verify(bookingRepository, times(2)).save(any(Booking.class)); // Unknown + updated
    }

    @Test
    @DisplayName("reportSpotTaken should throw exception when no spots available")
    void reportSpotTaken_whenNoSpotsAvailable_shouldThrowException() {
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(parkingRepository.findByAvailability("available"))
            .thenReturn(Collections.emptyList());

        assertThrows(ParkingNotAvailableException.class, () ->
            bookingService.reportSpotTaken(1)
        );
    }

    @Test
    @DisplayName("reportSpotTaken should throw exception when booking not active")
    void reportSpotTaken_whenNotActive_shouldThrowException() {
        testBooking.setStatus("Cancelled");
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        assertThrows(IllegalStateException.class, () ->
            bookingService.reportSpotTaken(1)
        );
    }

    @Test
    @DisplayName("reportSpotTaken should throw exception when no current spot")
    void reportSpotTaken_whenNoCurrentSpot_shouldThrowException() {
        testBooking.setParkingSpot(null);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        assertThrows(IllegalStateException.class, () ->
            bookingService.reportSpotTaken(1)
        );
    }
}
