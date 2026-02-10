package com.ahmedbahaj.parking.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Parking")
public class Parking {
    @Id
    @Column(name = "parking_id")
    private String parkingId;

    @Column(name = "availability", nullable = false)
    private String availability = "available";
}
