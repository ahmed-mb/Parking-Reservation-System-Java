package com.ahmedbahaj.parking.security;

import com.ahmedbahaj.parking.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Validates Bearer JWTs and populates the SecurityContext.
 *
 * Hardening notes:
 *  - All JJWT exceptions are caught defensively so a malformed token can never
 *    leak a stack trace to clients or break the filter chain.
 *  - Token parsing is performed once and the role claim is read from the
 *    already-parsed token, avoiding redundant signature checks.
 *  - The SecurityContext is cleared on validation failure to prevent stale
 *    authentication from leaking across requests on the same thread (Tomcat
 *    threads are pooled).
 *  - The token's {@code tv} claim is compared against the user's current
 *    {@code tokenVersion} in the DB (one scalar lookup per request). A
 *    mismatch — or no user found at all, e.g. the account was deleted —
 *    rejects the token even though its signature and expiry are still valid.
 *    This is what makes role changes and account deletion take effect
 *    immediately instead of waiting out the token's expiry window.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null
                    && Boolean.TRUE.equals(jwtUtil.validateToken(token, username))) {
                String role = jwtUtil.extractRole(token);
                if (role == null || role.isBlank()) {
                    // Token has no role claim - reject it, do not authenticate.
                    logger.warn("JWT missing role claim - request denied");
                    filterChain.doFilter(request, response);
                    return;
                }

                Optional<Integer> currentTokenVersion = userRepository.findTokenVersionByEmail(username);
                if (currentTokenVersion.isEmpty()) {
                    // User no longer exists (deleted since the token was issued).
                    logger.warn("Rejected JWT: user no longer exists");
                    filterChain.doFilter(request, response);
                    return;
                }
                Integer tokenVersion = jwtUtil.extractTokenVersion(token);
                if (tokenVersion == null || !tokenVersion.equals(currentTokenVersion.get())) {
                    // Token predates a role change (or similar) that bumped
                    // tokenVersion - treat it as revoked even though the
                    // signature and expiry are still valid.
                    logger.warn("Rejected JWT: token version mismatch");
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ex) {
            // Any parse / signature / expiration error - never authenticate.
            // Avoid leaking the actual exception to clients; just log a one-liner.
            SecurityContextHolder.clearContext();
            logger.warn("Rejected JWT: " + ex.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }
}
