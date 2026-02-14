package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.repository.ParkingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParkingService.
 */
@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {

    @Mock
    private ParkingRepository parkingRepository;

    @InjectMocks
    private ParkingService parkingService;

    private Parking testParking;

    @BeforeEach
    void setUp() {
        testParking = new Parking();
        testParking.setParkingId("A-001");
        testParking.setAvailability("available");
    }

    @Test
    @DisplayName("getAllParkingSpots should return all spots")
    void getAllParkingSpots_shouldReturnAllSpots() {
        Parking spot1 = new Parking();
        spot1.setParkingId("A-001");
        Parking spot2 = new Parking();
        spot2.setParkingId("A-002");
        
        when(parkingRepository.findAll()).thenReturn(Arrays.asList(spot1, spot2));
        
        List<Parking> result = parkingService.getAllParkingSpots();
        
        assertEquals(2, result.size());
        verify(parkingRepository).findAll();
    }

    @Test
    @DisplayName("getAvailableParkingSpots should return only available spots")
    void getAvailableParkingSpots_shouldReturnAvailableOnly() {
        when(parkingRepository.findByAvailability("available"))
            .thenReturn(Collections.singletonList(testParking));
        
        List<Parking> result = parkingService.getAvailableParkingSpots();
        
        assertEquals(1, result.size());
        assertEquals("A-001", result.get(0).getParkingId());
        verify(parkingRepository).findByAvailability("available");
    }

    @Test
    @DisplayName("getAvailableCount should return count of available spots")
    void getAvailableCount_shouldReturnCount() {
        when(parkingRepository.countByAvailability("available")).thenReturn(5L);
        
        long count = parkingService.getAvailableCount();
        
        assertEquals(5L, count);
        verify(parkingRepository).countByAvailability("available");
    }

    @Test
    @DisplayName("getParkingSpotById should return spot when exists")
    void getParkingSpotById_whenExists_shouldReturnSpot() {
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));
        
        Parking result = parkingService.getParkingSpotById("A-001");
        
        assertNotNull(result);
        assertEquals("A-001", result.getParkingId());
    }

    @Test
    @DisplayName("getParkingSpotById should throw exception when not exists")
    void getParkingSpotById_whenNotExists_shouldThrowException() {
        when(parkingRepository.findById("X-999")).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
            parkingService.getParkingSpotById("X-999")
        );
    }

    @Test
    @DisplayName("createParkingSpot should create new spot")
    void createParkingSpot_shouldCreateNewSpot() {
        when(parkingRepository.existsById("D-001")).thenReturn(false);
        when(parkingRepository.save(any(Parking.class))).thenAnswer(i -> i.getArgument(0));
        
        Parking result = parkingService.createParkingSpot("D-001");
        
        assertNotNull(result);
        assertEquals("D-001", result.getParkingId());
        assertEquals("available", result.getAvailability());
        verify(parkingRepository).save(any(Parking.class));
    }

    @Test
    @DisplayName("createParkingSpot should throw exception when spot exists")
    void createParkingSpot_whenExists_shouldThrowException() {
        when(parkingRepository.existsById("A-001")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> 
            parkingService.createParkingSpot("A-001")
        );
        verify(parkingRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteParkingSpot should delete existing spot")
    void deleteParkingSpot_whenExists_shouldDelete() {
        when(parkingRepository.existsById("A-001")).thenReturn(true);
        
        parkingService.deleteParkingSpot("A-001");
        
        verify(parkingRepository).deleteById("A-001");
    }

    @Test
    @DisplayName("deleteParkingSpot should throw exception when not exists")
    void deleteParkingSpot_whenNotExists_shouldThrowException() {
        when(parkingRepository.existsById("X-999")).thenReturn(false);
        
        assertThrows(ResourceNotFoundException.class, () -> 
            parkingService.deleteParkingSpot("X-999")
        );
    }

    @Test
    @DisplayName("updateParkingStatus should update status")
    void updateParkingStatus_shouldUpdateStatus() {
        when(parkingRepository.findById("A-001")).thenReturn(Optional.of(testParking));
        when(parkingRepository.save(any(Parking.class))).thenAnswer(i -> i.getArgument(0));
        
        Parking result = parkingService.updateParkingStatus("A-001", "booked");
        
        assertEquals("booked", result.getAvailability());
        verify(parkingRepository).save(testParking);
    }

    @Test
    @DisplayName("updateParkingStatus should throw exception when spot not found")
    void updateParkingStatus_whenNotFound_shouldThrowException() {
        when(parkingRepository.findById("X-999")).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
            parkingService.updateParkingStatus("X-999", "booked")
        );
    }

    @Test
    @DisplayName("initializeParkingSpots should create default spots")
    void initializeParkingSpots_shouldCreateDefaultSpots() {
        when(parkingRepository.existsById(anyString())).thenReturn(false);
        when(parkingRepository.save(any(Parking.class))).thenAnswer(i -> i.getArgument(0));
        
        parkingService.initializeParkingSpots();
        
        verify(parkingRepository, times(10)).save(any(Parking.class));
    }

    @Test
    @DisplayName("initializeParkingSpots should not create duplicate spots")
    void initializeParkingSpots_shouldNotCreateDuplicates() {
        when(parkingRepository.existsById(anyString())).thenReturn(true);
        
        parkingService.initializeParkingSpots();
        
        verify(parkingRepository, never()).save(any(Parking.class));
    }

    @Test
    @DisplayName("resetAllParkingSpots should call repository reset")
    void resetAllParkingSpots_shouldCallRepositoryReset() {
        parkingService.resetAllParkingSpots();
        
        verify(parkingRepository).resetAllSpots();
    }
}
