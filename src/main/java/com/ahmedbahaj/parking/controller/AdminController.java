package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.repository.ParkingRepository;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('Admin')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ParkingRepository parkingRepository;
    private final UserService userService;

    // ========== USER MANAGEMENT ==========

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        try {
            userRepository.deleteById(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/{id}/credit")
    public ResponseEntity<?> addCreditToUser(@PathVariable Integer id, @RequestParam BigDecimal amount) {
        try {
            userService.addCredit(id, amount);
            return ResponseEntity.ok("Credit added successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========== BOOKING MANAGEMENT ==========

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingRepository.findAll());
    }

    @GetMapping("/bookings/active")
    public ResponseEntity<List<Booking>> getActiveBookings() {
        return ResponseEntity.ok(bookingRepository.findByStatus("Active"));
    }

    @GetMapping("/bookings/user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(bookingRepository.findByUserId(userId));
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Integer id) {
        try {
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            String status = booking.getStatus();
            boolean isActive = status == null || "Active".equalsIgnoreCase(status);
            String userName = booking.getUserName();
            boolean isKnownUser = userName != null && !"Unknown".equalsIgnoreCase(userName)
                                  && booking.getUserId() != null && booking.getUserId() > 0;

            // Refund credit only for active bookings with known users
            if (isActive && isKnownUser) {
                userRepository.findById(booking.getUserId()).ifPresent(user -> {
                    user.setCredit(user.getCredit().add(new BigDecimal("6.00")));
                    userRepository.save(user);
                });
            }

            // Release parking spot if booked or unknown
            if (booking.getParkingSpot() != null) {
                parkingRepository.findById(booking.getParkingSpot()).ifPresent(parking -> {
                    String avail = parking.getAvailability();
                    if ("booked".equalsIgnoreCase(avail) || "unknown".equalsIgnoreCase(avail)) {
                        parking.setAvailability("available");
                        parkingRepository.save(parking);
                    }
                });
            }

            bookingRepository.deleteById(id);
            return ResponseEntity.ok("Booking deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========== PARKING MANAGEMENT ==========

    @GetMapping("/parking")
    public ResponseEntity<List<Parking>> getAllParkingSpots() {
        return ResponseEntity.ok(parkingRepository.findAll());
    }

    @GetMapping("/parking/stats")
    public ResponseEntity<Map<String, Object>> getParkingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", parkingRepository.count());
        stats.put("available", parkingRepository.countByAvailability("available"));
        stats.put("booked", parkingRepository.countByAvailability("booked"));
        stats.put("unknown", parkingRepository.countByAvailability("unknown"));
        return ResponseEntity.ok(stats);
    }

    // ========== DASHBOARD STATS ==========

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        stats.put("activeBookings", bookingRepository.findByStatus("Active").size());
        stats.put("totalParkingSpots", parkingRepository.count());
        stats.put("availableSpots", parkingRepository.countByAvailability("available"));
        return ResponseEntity.ok(stats);
    }
}
