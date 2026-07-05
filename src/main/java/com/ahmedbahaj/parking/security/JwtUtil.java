package com.ahmedbahaj.parking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Stateless JWT helper: issues and verifies the Bearer tokens used for
 * authentication. Tokens are signed with HMAC-SHA256 (HS256) using a shared
 * secret and carry the user's email as the subject plus a {@code role} claim.
 *
 * <p>Security notes:
 * <ul>
 *   <li>The signing secret comes from the {@code jwt.secret} property (env var
 *       {@code JWT_SECRET}) and <strong>must</strong> be at least 32 characters
 *       (256 bits) — HS256 derives no security from a shorter key. A too-short
 *       secret fails fast with {@link IllegalStateException} rather than
 *       silently weakening token security.</li>
 *   <li>Tokens carry a {@code tv} (token version) claim mirroring
 *       {@link com.ahmedbahaj.parking.model.User#getTokenVersion()}.
 *       {@code JwtAuthenticationFilter} compares this claim against the
 *       user's current DB value on every request, so bumping the DB value
 *       (done by {@code UserService} on role change, or implicitly by
 *       deleting the user) invalidates every outstanding token for that user
 *       immediately — expiry is no longer the only revocation mechanism.</li>
 *   <li>Claim extraction always parses with signature verification, so a token
 *       with a tampered payload or wrong signature is rejected before any claim
 *       is read.</li>
 * </ul>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    private SecretKey getSigningKey() {
        // Validate secret key length for HS256 (minimum 256 bits = 32 bytes)
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters. Set the JWT_SECRET environment variable.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Issues a new signed JWT for a successfully authenticated user.
     *
     * @param email        the user's email, stored as the token subject
     * @param role         the user's role, stored as a custom {@code role} claim
     * @param tokenVersion the user's current {@code tokenVersion}, stored as
     *                     a custom {@code tv} claim so this token can be
     *                     invalidated early if the version is later bumped
     * @return a compact, signed JWT string
     */
    public String generateToken(String email, String role, Integer tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("tv", tokenVersion);
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extracts the {@code tv} (token version) claim used by
     * {@code JwtAuthenticationFilter} to detect revoked tokens.
     *
     * @param token the signed JWT
     * @return the token-version number embedded in the token
     */
    public Integer extractTokenVersion(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("tv", Integer.class);
    }
}
