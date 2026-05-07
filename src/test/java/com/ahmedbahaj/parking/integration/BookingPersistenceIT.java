package com.ahmedbahaj.parking.integration;

import com.ahmedbahaj.parking.exception.ParkingNotAvailableException;
import com.ahmedbahaj.parking.model.Booking;
import com.ahmedbahaj.parking.model.Parking;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.BookingRepository;
import com.ahmedbahaj.parking.repository.ParkingRepository;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that exercises the booking flow against a real SQL Server,
 * not the H2 in-memory database used by the unit-test suite.
 *
 * <p><b>Why this exists.</b> H2 and SQL Server agree syntactically but diverge
 * on identity columns, datetime precision, reserved words, and lock semantics.
 * In particular, the pessimistic-write lock added in {@code BookingService}
 * relies on {@code SELECT ... FOR UPDATE}-style row locking. H2's MVCC mode
 * silently no-ops it; SQL Server actually serialises concurrent transactions.
 * Without this test, the double-booking regression we just fixed could quietly
 * come back when someone touches the JPA layer.
 *
 * <p><b>How it runs.</b> Testcontainers boots a real
 * {@code mcr.microsoft.com/mssql/server:2022-latest} in a Docker container,
 * Spring Boot is rewired to point at it via {@link DynamicPropertySource},
 * and the regular Spring context starts up against it. The container is
 * shared across tests in this class for speed.
 *
 * <p><b>When it runs.</b> Naming convention {@code *IT.java} binds this
 * to the failsafe / verify phase. Plain {@code mvn test} (used by IDE
 * runners and the unit-test job) skips it; {@code mvn verify} (used by CI)
 * runs it. This means devs don't need Docker locally for a quick test
 * cycle, but CI exercises the production database engine on every push.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Booking flow against real SQL Server (Testcontainers)")
class BookingPersistenceIT {

    @Container
    @SuppressWarnings("resource") // Lifecycle managed by Testcontainers via @Container.
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    /**
     * Rewires the Spring datasource at boot time to point at the
     * Testcontainers SQL Server instance. Also overrides the Hibernate
     * dialect (H2 -> SQL Server) and switches ddl-auto to {@code create-drop}
     * so the integration database starts empty.
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        registry.add("spring.datasource.driverClassName",
                () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.SQLServerDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Keep test JWT secret deterministic so SecurityConfig's
        // pre-flight length check (>= 32 chars) passes.
        registry.add("jwt.secret",
                () -> "IT-secret-key-for-jwt-token-generation-must-be-long-enough");
        registry.add("demo.mode", () -> "true");
    }

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private ParkingRepository parkingRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Integer userId;

    @BeforeEach
    void seed() {
        bookingRepository.deleteAll();
        parkingRepository.deleteAll();
        userRepository.deleteAll();

        User u = new User();
        u.setUsername("alice");
        u.setEmail("alice@example.com");
        u.setPassword(passwordEncoder.encode("Password@123"));
        u.setMobile("555-1234");
        u.setAddress("1 Main St");
        u.setCarPlateNo("ABC-123");
        u.setCredit(new BigDecimal("100.00"));
        u.setRole("Customer");
        userId = userRepository.save(u).getId();

        Parking p = new Parking();
        p.setParkingId("A-001");
        p.setAvailability("available");
        parkingRepository.save(p);
    }

    @Test
    @DisplayName("happy path: a single booking succeeds and decrements credit")
    void singleBooking_succeeds() {
        Booking b = bookingService.createBooking(userId, "A-001", "ABC-123");
        assertNotNull(b.getId());
        assertEquals("Active", b.getStatus());
        assertEquals("A-001", b.getParkingSpot());

        User refreshed = userRepository.findById(userId).orElseThrow();
        assertEquals(0, new BigDecimal("94.00").compareTo(refreshed.getCredit()),
                "Credit should be 100 - 6 = 94");

        Parking refreshedSpot = parkingRepository.findById("A-001").orElseThrow();
        assertEquals("booked", refreshedSpot.getAvailability());
    }

    @Test
    @DisplayName("concurrent bookings on the same spot: exactly one wins")
    void concurrentBookings_onlyOneWins() throws Exception {
        // Spawn two threads that race to book the same parking spot. Without
        // the pessimistic-write lock added in the audit fix, both could read
        // availability == "available" and both would proceed to write. With
        // the lock, the second transaction blocks until the first commits,
        // then re-reads availability == "booked" and throws.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        // Top up the user so credit is not the limiting factor.
        User u = userRepository.findById(userId).orElseThrow();
        u.setCredit(new BigDecimal("100.00"));
        userRepository.save(u);

        Runnable attempt = () -> {
            try {
                bookingService.createBooking(userId, "A-001", "ABC-123");
                successes.incrementAndGet();
            } catch (ParkingNotAvailableException expected) {
                conflicts.incrementAndGet();
            } catch (Exception other) {
                // Any other exception is a real failure; rethrow via JUnit.
                throw new AssertionError("Unexpected exception type", other);
            }
        };

        CompletableFuture<Void> a = CompletableFuture.runAsync(attempt, pool);
        CompletableFuture<Void> b = CompletableFuture.runAsync(attempt, pool);
        CompletableFuture.allOf(a, b).get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, successes.get(), "Exactly one booking should succeed");
        assertEquals(1, conflicts.get(), "Exactly one booking should hit ParkingNotAvailableException");

        List<Booking> created = bookingRepository.findAll();
        assertEquals(1, created.size(), "Database must contain exactly one booking row");
    }

    @Test
    @DisplayName("schema validates against SQL Server dialect (smoke)")
    void schemaValidatesAgainstSqlServer() {
        // If JPA / Hibernate could not generate the schema against SQL Server
        // (datetime precision, identity columns, reserved words), the Spring
        // context above would have failed to start and we would never have
        // reached this assertion. So the mere fact that this test runs is
        // the smoke test.
        assertTrue(SQL_SERVER.isRunning());
        assertNotNull(userRepository.findById(userId).orElseThrow().getEmail());
    }
}
