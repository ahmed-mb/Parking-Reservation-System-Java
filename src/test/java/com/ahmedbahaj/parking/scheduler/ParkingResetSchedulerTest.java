package com.ahmedbahaj.parking.scheduler;

import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingResetSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingService parkingService;

    @InjectMocks
    private ParkingResetScheduler scheduler;

    private List<Booking> activeBookings;

    @BeforeEach
    void setUp() {
        activeBookings = new ArrayList<>();
        
        Booking booking1 = new Booking();
        booking1.setId(1);
        booking1.setStatus("Active");
        
        Booking booking2 = new Booking();
        booking2.setId(2);
        booking2.setStatus("Active");
        
        activeBookings.add(booking1);
        activeBookings.add(booking2);
    }

    @Test
    @DisplayName("resetParkingAndBookings - should complete active bookings and reset spots")
    void resetParkingAndBookings_shouldCompleteBookingsAndResetSpots() {
        when(bookingRepository.findByStatus("Active")).thenReturn(activeBookings);
        when(bookingRepository.saveAll(anyList())).thenReturn(activeBookings);
        doNothing().when(parkingService).resetAllParkingSpots();

        scheduler.resetParkingAndBookings();

        verify(bookingRepository).findByStatus("Active");
        verify(bookingRepository).saveAll(anyList());
        verify(parkingService).resetAllParkingSpots();
        
        // Verify bookings are marked as Completed
        for (Booking booking : activeBookings) {
            assertEquals("Completed", booking.getStatus());
            assertNotNull(booking.getModifiedDate());
        }
    }

    @Test
    @DisplayName("resetParkingAndBookings - should handle empty booking list")
    void resetParkingAndBookings_shouldHandleEmptyList() {
        when(bookingRepository.findByStatus("Active")).thenReturn(List.of());
        when(bookingRepository.saveAll(anyList())).thenReturn(List.of());
        doNothing().when(parkingService).resetAllParkingSpots();

        scheduler.resetParkingAndBookings();

        verify(bookingRepository).findByStatus("Active");
        verify(bookingRepository).saveAll(anyList());
        verify(parkingService).resetAllParkingSpots();
    }

    @Test
    @DisplayName("resetParkingAndBookings - should propagate exception so @Transactional rolls back")
    void resetParkingAndBookings_shouldPropagateException() {
        when(bookingRepository.findByStatus("Active")).thenThrow(new RuntimeException("Database error"));

        // Must propagate - a caught-and-swallowed exception would mean
        // Spring's transaction manager never sees the failure and commits
        // whatever was already flushed, breaking the atomicity this method
        // is documented to provide.
        assertThrows(RuntimeException.class, () -> scheduler.resetParkingAndBookings());

        verify(bookingRepository).findByStatus("Active");
        verify(parkingService, never()).resetAllParkingSpots();
    }
}
