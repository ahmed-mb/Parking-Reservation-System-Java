package com.ahmedbahaj.parking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecaptchaService.
 * Tests reCAPTCHA verification and demo mode bypass.
 */
class RecaptchaServiceTest {

    private RecaptchaService recaptchaService;

    @BeforeEach
    void setUp() {
        recaptchaService = new RecaptchaService();
        ReflectionTestUtils.setField(recaptchaService, "secretKey", "test-secret-key");
    }

    @Test
    @DisplayName("Demo mode should bypass reCAPTCHA verification")
    void verifyRecaptcha_inDemoMode_shouldReturnTrue() {
        ReflectionTestUtils.setField(recaptchaService, "demoMode", true);
        
        boolean result = recaptchaService.verifyRecaptcha("any-token");
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Demo mode should bypass even with null token")
    void verifyRecaptcha_inDemoModeWithNullToken_shouldReturnTrue() {
        ReflectionTestUtils.setField(recaptchaService, "demoMode", true);
        
        boolean result = recaptchaService.verifyRecaptcha(null);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Demo mode should bypass even with empty token")
    void verifyRecaptcha_inDemoModeWithEmptyToken_shouldReturnTrue() {
        ReflectionTestUtils.setField(recaptchaService, "demoMode", true);
        
        boolean result = recaptchaService.verifyRecaptcha("");
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Non-demo mode with null token should return false")
    void verifyRecaptcha_notDemoModeWithNullToken_shouldReturnFalse() {
        ReflectionTestUtils.setField(recaptchaService, "demoMode", false);
        
        boolean result = recaptchaService.verifyRecaptcha(null);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Non-demo mode with empty token should return false")
    void verifyRecaptcha_notDemoModeWithEmptyToken_shouldReturnFalse() {
        ReflectionTestUtils.setField(recaptchaService, "demoMode", false);
        
        boolean result = recaptchaService.verifyRecaptcha("");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Non-demo mode with invalid token should return false (network error)")
    void verifyRecaptcha_notDemoModeWithInvalidToken_shouldReturnFalse() {
        ReflectionTestUtils.setField(recaptchaService, "demoMode", false);
        
        // This will fail because we're not mocking the RestTemplate
        // and the actual Google API won't accept our test token
        boolean result = recaptchaService.verifyRecaptcha("invalid-token");
        
        assertFalse(result);
    }
}
