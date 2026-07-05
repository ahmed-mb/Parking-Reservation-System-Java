package com.ahmedbahaj.parking.repository;

import com.ahmedbahaj.parking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    /**
     * Fetches only the {@code tokenVersion} scalar for a given email, without
     * loading the full entity (password hash included). Used by
     * {@code JwtAuthenticationFilter} on every authenticated request, so
     * avoiding a full-entity load keeps that per-request cost minimal.
     *
     * @param email the user's email
     * @return the current token version, or empty if no such user exists
     *         (e.g. the account was deleted after the token was issued)
     */
    @Query("SELECT u.tokenVersion FROM User u WHERE u.email = :email")
    Optional<Integer> findTokenVersionByEmail(@Param("email") String email);
}
