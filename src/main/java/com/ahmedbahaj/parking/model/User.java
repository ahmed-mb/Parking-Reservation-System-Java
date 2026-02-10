package com.ahmedbahaj.parking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "logintable")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Username", nullable = false)
    private String username;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @JsonIgnore // Security: Never expose password hash in API responses
    @Column(name = "pass", nullable = false)
    private String password;

    @Column(name = "Mobile")
    private String mobile;

    @Column(name = "Address")
    private String address;

    @Column(name = "Car_Plate_No")
    private String carPlateNo;

    @Column(name = "Credit")
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "Role", nullable = false)
    private String role = "Customer";
}
