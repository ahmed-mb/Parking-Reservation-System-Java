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

    @Scheduled(cron = "0 0 1 * * ?", zone = "America/New_York")
    @Transactional
    public void resetParkingAndBookings() {
        log.info("Starting daily parking reset");

        try {
            List<Booking> activeBookings = bookingRepository.findByStatus("Active");
            for (Booking booking : activeBookings) {
                booking.setStatus("Completed");
                booking.setModifiedDate(LocalDateTime.now());
            }
            bookingRepository.saveAll(activeBookings);

            parkingService.resetAllParkingSpots();

            log.info("Daily reset complete: {} bookings completed, all spots available", activeBookings.size());
        } catch (Exception e) {
            log.error("Error during parking reset: {}", e.getMessage(), e);
        }
    }
}
