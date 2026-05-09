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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ParkingRepository parkingRepository;

    private static final BigDecimal BOOKING_COST = new BigDecimal("6.00");
    private static final BigDecimal CANCELLATION_REFUND = new BigDecimal("6.00");

    @Transactional
    public Booking createBooking(Integer userId, String parkingId, String carPlate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCredit().compareTo(BOOKING_COST) < 0) {
            throw new InsufficientCreditException("Insufficient credit. Balance: " + user.getCredit());
        }

        // Pessimistic write-lock the parking row to prevent the classic
        // double-booking race: two requests reading "available" at the same
        // millisecond and both proceeding to set "booked" on the same spot.
        Parking parking = parkingRepository.findByIdForUpdate(parkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking spot not found"));

        if (!"available".equalsIgnoreCase(parking.getAvailability())) {
            throw new ParkingNotAvailableException("Parking spot is not available");
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setUserName(user.getUsername());
        booking.setUserContact(user.getMobile());
        booking.setCarPlate(carPlate);
        booking.setParkingSpot(parkingId);
        booking.setDate(LocalDateTime.now());
        booking.setStatus("Active");
        booking.setCredit(BOOKING_COST.intValue());

        user.setCredit(user.getCredit().subtract(BOOKING_COST));
        parking.setAvailability("booked");

        userRepository.save(user);
        parkingRepository.save(parking);

        return bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!"Active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is already cancelled or completed");
        }

        User user = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setCredit(user.getCredit().add(CANCELLATION_REFUND));
        userRepository.save(user);

        parkingRepository.findById(booking.getParkingSpot()).ifPresent(parking -> {
            parking.setAvailability("available");
            parkingRepository.save(parking);
        });

        booking.setStatus("Cancelled");
        booking.setCancelledDate(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    /**
     * Report Spot Taken - Migrated from ASP.NET C_userBooking.aspx.cs
     * Marks old spot as "unknown", creates an "Unknown" booking record,
     * finds and assigns a new available spot. No credit charge.
     */
    @Transactional
    public Booking reportSpotTaken(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!"Active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only active bookings can report spot taken");
        }

        String currentSpot = booking.getParkingSpot();
        if (currentSpot == null || currentSpot.isEmpty()) {
            throw new IllegalStateException("Unable to find your current parking spot assignment");
        }

        List<Parking> availableSpots = parkingRepository.findByAvailability("available");
        if (availableSpots.isEmpty()) {
            throw new ParkingNotAvailableException(
                "No available parking spots at the moment. Please contact support for assistance."
            );
        }

        String newSpot = availableSpots.get(0).getParkingId();

        // Mark old spot as "unknown"
        Parking oldParking = parkingRepository.findById(currentSpot)
                .orElseThrow(() -> new ResourceNotFoundException("Old parking spot not found"));
        oldParking.setAvailability("unknown");
        parkingRepository.save(oldParking);

        // Create "Unknown" booking record for the old spot
        Booking unknownBooking = new Booking();
        unknownBooking.setUserId(0);
        unknownBooking.setUserName("Unknown");
        unknownBooking.setUserContact("Unknown");
        unknownBooking.setCarPlate("Unknown");
        unknownBooking.setParkingSpot(currentSpot);
        unknownBooking.setDate(LocalDateTime.now());
        unknownBooking.setStatus("Unknown");
        unknownBooking.setCredit(0);
        unknownBooking.setCreatedDate(LocalDateTime.now());
        bookingRepository.save(unknownBooking);

        // Book the new spot
        Parking newParking = parkingRepository.findById(newSpot)
                .orElseThrow(() -> new ResourceNotFoundException("New parking spot not found"));
        newParking.setAvailability("booked");
        parkingRepository.save(newParking);

        // Update booking with the new spot
        booking.setParkingSpot(newSpot);
        booking.setModifiedDate(LocalDateTime.now());
        return bookingRepository.save(booking);
    }
}
