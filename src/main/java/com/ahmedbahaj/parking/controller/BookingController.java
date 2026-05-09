package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.dto.BookingRequest;
import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.service.BookingService;
import com.ahmedbahaj.parking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Booking endpoints. All authorization is enforced server-side; client claims
 * about user identity are ignored in favour of the authenticated principal.
 *
 * Note on error handling: this controller intentionally avoids `catch (Exception)`
 * blocks. Specific exceptions (ResourceNotFoundException, AccessDeniedException,
 * ParkingNotAvailableException, etc.) are translated into proper HTTP status
 * codes by GlobalExceptionHandler, ensuring consistent error envelopes across
 * the API and avoiding leakage of internal stack traces or vendor-specific
 * messages to clients.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final String ROLE_ADMIN = "Admin";

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserService userService;

    /** List every booking. Admin-only. */
    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /** Create a booking for the authenticated user (or any user if Admin). */
    @PostMapping
    public Booking createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());

        if (!currentUser.getId().equals(request.getUserId()) && !ROLE_ADMIN.equals(currentUser.getRole())) {
            throw new AccessDeniedException("You can only create bookings for yourself");
        }

        return bookingService.createBooking(
                request.getUserId(), request.getParkingId(), request.getCarPlate());
    }

    /** Cancel a booking owned by the authenticated user (or any booking if Admin). */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Integer id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUserId().equals(currentUser.getId()) && !ROLE_ADMIN.equals(currentUser.getRole())) {
            throw new AccessDeniedException("You can only cancel your own bookings");
        }

        bookingService.cancelBooking(id);
        return "Booking cancelled successfully";
    }

    /** List bookings for a specific user. Owner or Admin only. */
    @GetMapping("/user/{userId}")
    public List<Booking> getUserBookings(
            @PathVariable Integer userId,
            Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());

        if (!currentUser.getId().equals(userId) && !ROLE_ADMIN.equals(currentUser.getRole())) {
            throw new AccessDeniedException("You can only view your own bookings");
        }

        return bookingRepository.findByUserId(userId);
    }

    /** List currently active bookings. Admin-only. */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('Admin')")
    public List<Booking> getActiveBookings() {
        return bookingRepository.findByStatus("Active");
    }

    /** The authenticated user's active booking(s). */
    @GetMapping("/my-active")
    public List<Booking> getMyActiveBooking(Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        return bookingRepository.findByUserIdAndStatus(currentUser.getId(), "Active");
    }

    /** Report the user's currently-assigned spot as taken; reassigns to a free one. */
    @PostMapping("/{id}/report-taken")
    public Booking reportSpotTaken(@PathVariable Integer id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only report your own booking's spot as taken");
        }

        return bookingService.reportSpotTaken(id);
    }
}
