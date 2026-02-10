package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.dto.BookingRequest;
import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.service.BookingService;
import com.ahmedbahaj.parking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserService userService;

    /**
     * Get all bookings - ADMIN ONLY
     * Security: Regular users should only see their own bookings
     */
    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Create a new booking
     * Security: Users can only create bookings for themselves
     */
    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            
            // Security check: Users can only create bookings for themselves
            if (!currentUser.getId().equals(request.getUserId()) && !"Admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only create bookings for yourself");
            }
            
            Booking booking = bookingService.createBooking(
                request.getUserId(), request.getParkingId(), request.getCarPlate()
            );
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Cancel a booking
     * Security: Users can only cancel their OWN bookings, Admin can cancel any
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            // Security check: Only owner or Admin can cancel
            if (!booking.getUserId().equals(currentUser.getId()) && !"Admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only cancel your own bookings");
            }
            
            bookingService.cancelBooking(id);
            return ResponseEntity.ok("Booking cancelled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get bookings for a specific user
     * Security: Users can only view their OWN bookings, Admin can view any
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBookings(
            @PathVariable Integer userId,
            Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            
            // Security check: Only owner or Admin can view
            if (!currentUser.getId().equals(userId) && !"Admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only view your own bookings");
            }
            
            return ResponseEntity.ok(bookingRepository.findByUserId(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get active bookings - ADMIN ONLY
     * Security: Only admins should see all active bookings
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<List<Booking>> getActiveBookings() {
        return ResponseEntity.ok(bookingRepository.findByStatus("Active"));
    }

    /**
     * Get current user's active booking
     * Security: Users can only view their own active booking
     */
    @GetMapping("/my-active")
    public ResponseEntity<?> getMyActiveBooking(Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            List<Booking> activeBookings = bookingRepository.findByUserIdAndStatus(
                    currentUser.getId(), "Active");
            return ResponseEntity.ok(activeBookings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Report a spot as taken
     * Security: Users can only report their OWN booking's spot as taken
     */
    @PostMapping("/{id}/report-taken")
    public ResponseEntity<?> reportSpotTaken(
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            // Security check: Only owner can report spot taken
            if (!booking.getUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only report your own booking's spot as taken");
            }
            
            return ResponseEntity.ok(bookingService.reportSpotTaken(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
