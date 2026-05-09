package com.ahmedbahaj.parking.dto;

import com.ahmedbahaj.parking.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link UserResponse} cannot accidentally leak the password
 * hash even if a future code change removes {@code @JsonIgnore} from the
 * {@link User} entity. This is the regression guard for the "DTO surface
 * contract" hardening.
 */
class UserResponseTest {

    @Test
    @DisplayName("from() copies only the safe fields")
    void from_copiesSafeFieldsOnly() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$12$abc"); // hashed password - must not appear in DTO
        user.setMobile("555-1234");
        user.setAddress("1 Main St");
        user.setCarPlateNo("ABC-123");
        user.setCredit(new BigDecimal("12.34"));
        user.setRole("Customer");

        UserResponse dto = UserResponse.from(user);

        assertEquals(1, dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("555-1234", dto.getMobile());
        assertEquals("1 Main St", dto.getAddress());
        assertEquals("ABC-123", dto.getCarPlateNo());
        assertEquals(new BigDecimal("12.34"), dto.getCredit());
        assertEquals("Customer", dto.getRole());
    }

    @Test
    @DisplayName("from(null) returns null safely")
    void from_nullReturnsNull() {
        assertNull(UserResponse.from(null));
    }

    @Test
    @DisplayName("serialised JSON never contains a password field")
    void json_doesNotIncludePassword() throws Exception {
        User user = new User();
        user.setId(2);
        user.setUsername("bob");
        user.setEmail("bob@example.com");
        user.setPassword("$2a$12$leak-should-never-happen");

        String json = new ObjectMapper().writeValueAsString(UserResponse.from(user));
        assertFalse(json.toLowerCase().contains("password"),
            "UserResponse JSON must never include a password field. Got: " + json);
        assertFalse(json.contains("$2a$12$leak-should-never-happen"),
            "UserResponse JSON must never include the raw password hash. Got: " + json);
    }
}
