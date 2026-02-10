package com.ahmedbahaj.parking.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Booking")
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
