package com.ahmedbahaj.parking.scheduler;

import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingResetScheduler {

    private final BookingRepository bookingRepository;
    private final ParkingService parkingService;

    /**
     * Runs daily at 01:00 America/New_York: marks every "Active" booking as
     * "Completed" and resets all parking spots to "available".
     *
     * <p>Runs in a single transaction so the booking updates and spot reset
     * commit together. Exceptions are deliberately left to propagate — a
     * previous version caught and logged them here, which meant
     * {@code @Transactional} never saw a failure to roll back on: a partial
     * failure (e.g. the booking updates succeeding but the spot reset
     * failing) would silently commit the half-done state instead of rolling
     * back. Letting the exception propagate restores that guarantee; Spring's
     * scheduler logs the failure and simply skips to the next scheduled run.
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "America/New_York")
    @Transactional
    public void resetParkingAndBookings() {
        log.info("Starting daily parking reset");

        List<Booking> activeBookings = bookingRepository.findByStatus("Active");
        for (Booking booking : activeBookings) {
            booking.setStatus("Completed");
            booking.setModifiedDate(LocalDateTime.now());
        }
        bookingRepository.saveAll(activeBookings);

        parkingService.resetAllParkingSpots();

        log.info("Daily reset complete: {} bookings completed, all spots available", activeBookings.size());
    }
}
