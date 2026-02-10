package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.dto.LoginRequest;
import com.ahmedbahaj.parking.dto.LoginResponse;
import com.ahmedbahaj.parking.dto.RegisterRequest;
import com.ahmedbahaj.parking.exception.DuplicateEmailException;
import com.ahmedbahaj.parking.exception.InvalidCredentialsException;
import com.ahmedbahaj.parking.exception.ResourceNotFoundException;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RecaptchaService recaptchaService;
    private final PasswordValidator passwordValidator;

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutes

    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        log.info("Login attempt for: {}", email);

        if (!recaptchaService.verifyRecaptcha(request.getRecaptchaToken())) {
            log.warn("reCAPTCHA verification failed for: {}", email);
            throw new InvalidCredentialsException("reCAPTCHA verification failed. Please try again.");
        }

        if (isAccountLocked(email)) {
            log.warn("Account locked: {}", email);
            throw new InvalidCredentialsException("Account is locked due to too many failed attempts. Try again in 15 minutes.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", email);
            recordFailedAttempt(email);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedAttempt(email);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        clearFailedAttempts(email);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("Login successful for: {}", email);

        return new LoginResponse(token, user.getEmail(), user.getUsername(), user.getRole());
    }

    public User register(RegisterRequest request) {
        if (!recaptchaService.verifyRecaptcha(request.getRecaptchaToken())) {
            log.warn("reCAPTCHA verification failed for registration: {}", request.getEmail());
            throw new InvalidCredentialsException("reCAPTCHA verification failed. Please try again.");
        }

        passwordValidator.validate(request.getPassword(), request.getUsername());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMobile(request.getMobile());
        user.setAddress(request.getAddress());
        user.setCarPlateNo(request.getCarPlateNo());
        user.setCredit(BigDecimal.ZERO);
        user.setRole("Customer");

        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User updateUser(Integer id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setUsername(updatedUser.getUsername());
        user.setMobile(updatedUser.getMobile());
        user.setAddress(updatedUser.getAddress());
        user.setCarPlateNo(updatedUser.getCarPlateNo());

        if (!user.getEmail().equals(updatedUser.getEmail())) {
            if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new DuplicateEmailException("Email already exists");
            }
            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getCredit() != null) {
            user.setCredit(updatedUser.getCredit());
        }

        return userRepository.save(user);
    }

    public void addCredit(Integer userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setCredit(user.getCredit().add(amount));
        userRepository.save(user);
    }

    public void deductCredit(Integer userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getCredit().compareTo(amount) < 0) {
            throw new com.ahmedbahaj.parking.exception.InsufficientCreditException("Insufficient credit");
        }
        user.setCredit(user.getCredit().subtract(amount));
        userRepository.save(user);
    }

    // --- Login attempt tracking (thread-safe) ---

    private void recordFailedAttempt(String email) {
        loginAttempts.computeIfAbsent(email, k -> new LoginAttempt()).incrementAttempts();
    }

    private void clearFailedAttempts(String email) {
        loginAttempts.remove(email);
    }

    private boolean isAccountLocked(String email) {
        LoginAttempt attempt = loginAttempts.get(email);
        if (attempt == null) return false;

        if (attempt.isLocked()) {
            if (System.currentTimeMillis() - attempt.getLastAttemptTime() > LOCKOUT_DURATION) {
                clearFailedAttempts(email);
                return false;
            }
            return true;
        }
        return false;
    }

    private static class LoginAttempt {
        private int attempts = 0;
        private long lastAttemptTime;

        public synchronized void incrementAttempts() {
            attempts++;
            lastAttemptTime = System.currentTimeMillis();
        }

        public synchronized boolean isLocked() {
            return attempts >= MAX_ATTEMPTS;
        }

        public synchronized long getLastAttemptTime() {
            return lastAttemptTime;
        }
    }
}
