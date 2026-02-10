package com.ahmedbahaj.parking.config;

import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.service.ParkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final ParkingService parkingService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.password:admin}")
    private String adminDefaultPassword;

    public DatabaseInitializer(
            ParkingService parkingService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.parkingService = parkingService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing database...");

        parkingService.initializeParkingSpots();

        if (userRepository.findByEmail("admin@parking.com").isEmpty()) {
            // Security warning for default password
            if ("admin".equals(adminDefaultPassword)) {
                log.warn("=============================================================");
                log.warn("WARNING: Using default admin password!");
                log.warn("Set ADMIN_DEFAULT_PASSWORD environment variable in production.");
                log.warn("=============================================================");
            }

            User admin = new User();
            admin.setUsername("Admin");
            admin.setEmail("admin@parking.com");
            admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setMobile("1234567890");
            admin.setAddress("Admin Address");
            admin.setCarPlateNo("ADMIN-001");
            admin.setCredit(new BigDecimal("1000.00"));
            admin.setRole("Admin");
            userRepository.save(admin);

            log.info("Default admin user created: admin@parking.com");
            log.info("IMPORTANT: Change the admin password after first login!");
        }

        log.info("Database initialized: 10 parking spots (A-001 to C-004)");
    }
}
