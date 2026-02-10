package com.ahmedbahaj.parking.repository;

import com.ahmedbahaj.parking.model.Parking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParkingRepository extends JpaRepository<Parking, String> {
    List<Parking> findByAvailability(String availability);

    long countByAvailability(String availability);

    @Modifying
    @Query("UPDATE Parking p SET p.availability = 'available'")
    void resetAllSpots();
}
