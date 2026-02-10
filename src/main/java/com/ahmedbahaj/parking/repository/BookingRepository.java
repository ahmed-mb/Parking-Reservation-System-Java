package com.ahmedbahaj.parking.repository;

import com.ahmedbahaj.parking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByUserId(Integer userId);
    List<Booking> findByStatus(String status);
    List<Booking> findByUserIdAndStatus(Integer userId, String status);
}
