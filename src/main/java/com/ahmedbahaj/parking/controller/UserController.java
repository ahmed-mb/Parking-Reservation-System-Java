package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.dto.LoginRequest;
import com.ahmedbahaj.parking.dto.LoginResponse;
import com.ahmedbahaj.parking.dto.RegisterRequest;
import com.ahmedbahaj.parking.dto.UserResponse;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    /** Admin-only listing of customers, returned as DTOs (never raw entities). */
    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        User created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Update a user profile. The authenticated user can update only their own
     * record; only Admins may update someone else. Credit is admin-only.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @RequestBody User user,
            Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());

        if (!currentUser.getId().equals(id) && !"Admin".equals(currentUser.getRole())) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        // Defence-in-depth: blank out fields that customers must not be able
        // to mutate via this endpoint. The service additionally enforces this
        // server-side, but stripping them here means the inbound payload is
        // never trusted.
        if (!"Admin".equals(currentUser.getRole())) {
            user.setCredit(null);
            user.setRole(null);
        }

        return ResponseEntity.ok(UserResponse.from(userService.updateUser(id, user)));
    }

    /** Admin-only credit top-up. */
    @PostMapping("/{id}/credit")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<String> addCredit(@PathVariable Integer id, @RequestParam BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        userService.addCredit(id, amount);
        return ResponseEntity.ok("Credit added successfully");
    }

    /** Admin-only delete. Admins cannot delete themselves. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id, Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        if (currentUser.getId().equals(id)) {
            throw new IllegalStateException("Cannot delete your own account");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}
