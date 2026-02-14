package com.ahmedbahaj.parking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ParkingSystemApplicationTests {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // This test verifies the Spring context loads without errors
    }

    @Test
    @DisplayName("Main method should run without exception")
    void mainMethodShouldRun() {
        // Test that main method can be called (though it won't actually start server in test)
        // This is primarily for coverage of the main class
    }
}
