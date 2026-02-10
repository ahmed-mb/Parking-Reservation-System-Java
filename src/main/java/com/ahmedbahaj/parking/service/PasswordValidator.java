package com.ahmedbahaj.parking.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Password validation matching ASP.NET PasswordValidator.cs exactly.
 * OWASP password complexity recommendations.
 */
@Component
public class PasswordValidator {

    private static final int MINIMUM_LENGTH = 8;

    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "123456", "12345678", "qwerty", "abc123",
        "monkey", "1234567", "letmein", "trustno1", "dragon",
        "baseball", "iloveyou", "master", "sunshine", "ashley",
        "bailey", "passw0rd", "shadow", "123123", "654321",
        "superman", "qazwsx", "michael", "football", "admin"
    );

    /**
     * Validates password against security policy.
     * @throws IllegalArgumentException with specific message on failure
     */
    public void validate(String password, String username) {
        if (password == null || password.length() < MINIMUM_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MINIMUM_LENGTH + " characters long.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter (A-Z).");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter (a-z).");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one number (0-9).");
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>).");
        }
        if (username != null && !username.isEmpty() && password.toLowerCase().contains(username.toLowerCase())) {
            throw new IllegalArgumentException("Password cannot contain your username.");
        }
        if (COMMON_PASSWORDS.stream().anyMatch(cp -> cp.equalsIgnoreCase(password))) {
            throw new IllegalArgumentException("This password is too common. Please choose a more unique password.");
        }
    }
}
