package com.ahmedbahaj.parking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil.
 * Tests JWT token generation, extraction, and validation.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "TestSecretKeyForJWTTokenGenerationMustBeLongEnoughForHS256Algorithm!";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ROLE = "Customer";
    private static final Integer TEST_TOKEN_VERSION = 0;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
    }

    @Test
    @DisplayName("Should generate valid token")
    void generateToken_shouldReturnValidToken() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains(".")); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals(TEST_EMAIL, extractedUsername);
    }

    @Test
    @DisplayName("Should extract role from token")
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        String extractedRole = jwtUtil.extractRole(token);

        assertEquals(TEST_ROLE, extractedRole);
    }

    @Test
    @DisplayName("Should extract token version from token")
    void extractTokenVersion_shouldReturnCorrectVersion() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, 5);

        Integer extractedVersion = jwtUtil.extractTokenVersion(token);

        assertEquals(5, extractedVersion);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractExpiration_shouldReturnFutureDate() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Should validate token with correct username")
    void validateToken_withCorrectUsername_shouldReturnTrue() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        Boolean isValid = jwtUtil.validateToken(token, TEST_EMAIL);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should not validate token with incorrect username")
    void validateToken_withIncorrectUsername_shouldReturnFalse() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        Boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should throw exception for short secret key")
    void getSigningKey_withShortSecret_shouldThrowException() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "short");

        assertThrows(IllegalStateException.class, () ->
            jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION)
        );
    }

    @Test
    @DisplayName("Should throw exception for null secret key")
    void getSigningKey_withNullSecret_shouldThrowException() {
        ReflectionTestUtils.setField(jwtUtil, "secret", null);

        assertThrows(IllegalStateException.class, () ->
            jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION)
        );
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void generateToken_forDifferentUsers_shouldReturnDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1@example.com", "Customer", 0);
        String token2 = jwtUtil.generateToken("user2@example.com", "Admin", 0);

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should handle Admin role correctly")
    void generateToken_withAdminRole_shouldExtractAdminRole() {
        String token = jwtUtil.generateToken(TEST_EMAIL, "Admin", TEST_TOKEN_VERSION);

        assertEquals("Admin", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Token should not be expired immediately after generation")
    void validateToken_immediatelyAfterGeneration_shouldBeValid() {
        String token = jwtUtil.generateToken(TEST_EMAIL, TEST_ROLE, TEST_TOKEN_VERSION);

        Date expiration = jwtUtil.extractExpiration(token);

        // Token should expire in approximately 24 hours (86400000 ms)
        long expectedExpiry = System.currentTimeMillis() + 86400000L;
        long actualExpiry = expiration.getTime();

        // Allow 5 second tolerance
        assertTrue(Math.abs(expectedExpiry - actualExpiry) < 5000);
    }
}
