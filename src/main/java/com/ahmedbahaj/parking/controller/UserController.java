package com.ahmedbahaj.parking.controller;

import com.ahmedbahaj.parking.dto.LoginRequest;
import com.ahmedbahaj.parking.dto.LoginResponse;
import com.ahmedbahaj.parking.dto.RegisterRequest;
import com.ahmedbahaj.parking.model.User;
import com.ahmedbahaj.parking.repository.UserRepository;
import com.ahmedbahaj.parking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Get all users - ADMIN ONLY
     * Security: Requires Admin role to prevent user enumeration
     */
    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            User user = userService.getUserByEmail(authentication.getName());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update user profile
     * Security: Users can only update their OWN profile, unless Admin
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Integer id,
            @RequestBody User user,
            Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            
            // Security check: Only allow users to update their own profile, or Admin can update anyone
            if (!currentUser.getId().equals(id) && !"Admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only update your own profile");
            }
            
            // Prevent non-admins from updating credit through this endpoint
            if (!"Admin".equals(currentUser.getRole())) {
                user.setCredit(null); // Will be ignored in service
            }
            
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Add credit to user account - ADMIN ONLY
     * Security: Only admins can add credit to prevent fraud
     */
    @PostMapping("/{id}/credit")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> addCredit(@PathVariable Integer id, @RequestParam BigDecimal amount) {
        try {
            // Validate amount is positive
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Credit amount must be positive");
            }
            userService.addCredit(id, amount);
            return ResponseEntity.ok("Credit added successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Delete user - ADMIN ONLY
     * Security: Only admins can delete users
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id, Authentication authentication) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            
            // Prevent admin from deleting themselves
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest().body("Cannot delete your own account");
            }
            
            userRepository.deleteById(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
