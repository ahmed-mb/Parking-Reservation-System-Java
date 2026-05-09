package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.service.ParkingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public + admin-restricted parking endpoints.
 *
 * Security:
 *  - Read endpoints under /api/parking/available/** are intentionally public
 *    (used by the home page to display availability counts).
 *  - All mutating endpoints (POST/PUT/DELETE, initialize, reset) require the
 *    Admin authority. Without this, any authenticated Customer could create,
 *    delete, or reset parking spots — a critical authorization bypass.
 */
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<List<Parking>> getAllParkingSpots() {
        return ResponseEntity.ok(parkingService.getAllParkingSpots());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Parking>> getAvailableParkingSpots() {
        return ResponseEntity.ok(parkingService.getAvailableParkingSpots());
    }

    @GetMapping("/available/count")
    public ResponseEntity<Map<String, Object>> getAvailableCount() {
        return ResponseEntity.ok(Map.of("available", parkingService.getAvailableCount()));
    }

    @GetMapping("/{parkingId}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Parking> getParkingSpotById(
            @PathVariable
            @Pattern(regexp = "^[A-Z]-\\d{3}$", message = "Invalid parking spot id format")
            String parkingId) {
        return ResponseEntity.ok(parkingService.getParkingSpotById(parkingId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Parking> createParkingSpot(@Valid @RequestBody ParkingSpotRequest request) {
        return ResponseEntity.ok(parkingService.createParkingSpot(request.getParkingId()));
    }

    @PutMapping("/{parkingId}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Parking> updateParkingStatus(
            @PathVariable
            @Pattern(regexp = "^[A-Z]-\\d{3}$", message = "Invalid parking spot id format")
            String parkingId,
            @Valid @RequestBody ParkingStatusRequest request) {
        return ResponseEntity.ok(parkingService.updateParkingStatus(parkingId, request.getStatus()));
    }

    @DeleteMapping("/{parkingId}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<String> deleteParkingSpot(
            @PathVariable
            @Pattern(regexp = "^[A-Z]-\\d{3}$", message = "Invalid parking spot id format")
            String parkingId) {
        parkingService.deleteParkingSpot(parkingId);
        return ResponseEntity.ok("Parking spot deleted successfully");
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<String> initializeParkingSpots() {
        parkingService.initializeParkingSpots();
        return ResponseEntity.ok("Parking spots initialized successfully");
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<String> resetAllParkingSpots() {
        parkingService.resetAllParkingSpots();
        return ResponseEntity.ok("All parking spots reset to available");
    }

    /** Validated request body for creating a parking spot. */
    @Data
    public static class ParkingSpotRequest {
        @NotBlank(message = "parkingId is required")
        @Pattern(regexp = "^[A-Z]-\\d{3}$", message = "Invalid parking spot id format (e.g. A-001)")
        private String parkingId;
    }

    /** Validated request body for updating a parking spot's availability. */
    @Data
    public static class ParkingStatusRequest {
        @NotBlank(message = "status is required")
        @Pattern(regexp = "^(available|booked|unknown)$", message = "status must be one of: available, booked, unknown")
        private String status;
    }
}
