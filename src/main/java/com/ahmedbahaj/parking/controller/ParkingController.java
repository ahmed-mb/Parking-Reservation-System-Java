package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @GetMapping
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
    public ResponseEntity<?> getParkingSpotById(@PathVariable String parkingId) {
        try {
            return ResponseEntity.ok(parkingService.getParkingSpotById(parkingId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createParkingSpot(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(parkingService.createParkingSpot(request.get("parkingId")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{parkingId}")
    public ResponseEntity<?> updateParkingStatus(@PathVariable String parkingId, @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(parkingService.updateParkingStatus(parkingId, request.get("status")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{parkingId}")
    public ResponseEntity<?> deleteParkingSpot(@PathVariable String parkingId) {
        try {
            parkingService.deleteParkingSpot(parkingId);
            return ResponseEntity.ok("Parking spot deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeParkingSpots() {
        parkingService.initializeParkingSpots();
        return ResponseEntity.ok("Parking spots initialized successfully");
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetAllParkingSpots() {
        parkingService.resetAllParkingSpots();
        return ResponseEntity.ok("All parking spots reset to available");
    }
}
