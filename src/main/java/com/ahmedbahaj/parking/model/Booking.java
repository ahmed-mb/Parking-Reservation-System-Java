package com.ahmedbahaj.parking.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * A parking reservation: who booked, which spot, the car plate, and the
 * lifecycle timestamps/status ("Active", "Cancelled", etc.).
 *
 * <p>Snapshots the booker's name and contact at creation time so historical
 * records stay accurate even if the user's profile later changes. Maps to the
 * {@code Booking} table inherited from the original ASP.NET schema.
 *
 * <p>Rows are never deleted by the nightly reset (see
 * {@code ParkingResetScheduler}) — bookings are marked "Completed" instead —
 * so this table only grows. {@code user_id} and {@code status} are indexed
 * because {@link com.ahmedbahaj.parking.repository.BookingRepository}'s
 * {@code findByUserId}, {@code findByUserIdAndStatus}, and {@code findByStatus}
 * queries hit every customer dashboard load and every nightly reset run.
 */
@Data
@Entity
@Table(name = "Booking", indexes = {
    @Index(name = "idx_booking_user_id", columnList = "user_id"),
    @Index(name = "idx_booking_status", columnList = "status")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "user_contact", nullable = false)
    private String userContact;

    @Column(name = "credit", nullable = false)
    private Integer credit = 6;

    @Column(name = "car_plate", nullable = false)
    private String carPlate;

    @Column(name = "parking_spot")
    private String parkingSpot;

    @Column(name = "Date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status")
    private String status = "Active";

    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;

    @Column(name = "cancelled_reason")
    private String cancelledReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
