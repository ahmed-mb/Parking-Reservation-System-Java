package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.repository.ParkingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRepository parkingRepository;

    private static final String[] SPOT_IDS = {
        "A-001", "A-002", "A-003",
        "B-001", "B-002", "B-003",
        "C-001", "C-002", "C-003", "C-004"
    };

    public List<Parking> getAllParkingSpots() {
        return parkingRepository.findAll();
    }

    public List<Parking> getAvailableParkingSpots() {
        return parkingRepository.findByAvailability("available");
    }

    public long getAvailableCount() {
        return parkingRepository.countByAvailability("available");
    }

    public Parking getParkingSpotById(String parkingId) {
        return parkingRepository.findById(parkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking spot not found: " + parkingId));
    }

    public Parking createParkingSpot(String parkingId) {
        if (parkingRepository.existsById(parkingId)) {
            throw new IllegalArgumentException("Parking spot already exists: " + parkingId);
        }
        Parking parking = new Parking();
        parking.setParkingId(parkingId);
        parking.setAvailability("available");
        return parkingRepository.save(parking);
    }

    public void deleteParkingSpot(String parkingId) {
        if (!parkingRepository.existsById(parkingId)) {
            throw new ResourceNotFoundException("Parking spot not found: " + parkingId);
        }
        parkingRepository.deleteById(parkingId);
    }

    public Parking updateParkingStatus(String parkingId, String status) {
        Parking parking = parkingRepository.findById(parkingId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking spot not found: " + parkingId));
        parking.setAvailability(status);
        return parkingRepository.save(parking);
    }

    @Transactional
    public void initializeParkingSpots() {
        for (String spotId : SPOT_IDS) {
            if (!parkingRepository.existsById(spotId)) {
                Parking parking = new Parking();
                parking.setParkingId(spotId);
                parking.setAvailability("available");
                parkingRepository.save(parking);
            }
        }
    }

    @Transactional
    public void resetAllParkingSpots() {
        parkingRepository.resetAllSpots();
    }
}
