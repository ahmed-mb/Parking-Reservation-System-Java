package com.ahmedbahaj.parking.repository;

import com.ahmedbahaj.parking.model.Parking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingRepository extends JpaRepository<Parking, String> {
    List<Parking> findByAvailability(String availability);

    long countByAvailability(String availability);

    /**
     * Pessimistic write-lock variant of findById, used by BookingService to
     * prevent two concurrent bookings from both succeeding on the same spot.
     * Must run inside a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parking p WHERE p.parkingId = :id")
    Optional<Parking> findByIdForUpdate(@Param("id") String id);

    @Modifying
    @Query("UPDATE Parking p SET p.availability = 'available'")
    void resetAllSpots();
}
