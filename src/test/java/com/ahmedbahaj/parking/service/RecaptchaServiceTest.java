package com.ahmedbahaj.parking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecaptchaService.
 *
 * <p>There is no demo-mode bypass to test: verification is always enforced
 * regardless of profile (see the class javadoc on {@link RecaptchaService}
 * for why the old bypass was removed).
 */
class RecaptchaServiceTest {

    private RecaptchaService recaptchaService;

    @BeforeEach
    void setUp() {
        recaptchaService = new RecaptchaService();
        ReflectionTestUtils.setField(recaptchaService, "secretKey", "test-secret-key");
    }

    @Test
    @DisplayName("Null token should return false")
    void verifyRecaptcha_withNullToken_shouldReturnFalse() {
        boolean result = recaptchaService.verifyRecaptcha(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Empty token should return false")
    void verifyRecaptcha_withEmptyToken_shouldReturnFalse() {
        boolean result = recaptchaService.verifyRecaptcha("");

        assertFalse(result);
    }

    @Test
    @DisplayName("Invalid token should return false (network error / rejected by Google)")
    void verifyRecaptcha_withInvalidToken_shouldReturnFalse() {
        // This will fail because we're not mocking the RestTemplate
        // and the actual Google API won't accept our test token
        boolean result = recaptchaService.verifyRecaptcha("invalid-token");

        assertFalse(result);
    }
}
