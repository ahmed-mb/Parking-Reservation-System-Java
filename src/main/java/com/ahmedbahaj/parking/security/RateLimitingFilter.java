package com.ahmedbahaj.parking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coarse per-IP token-bucket rate limiter for authentication endpoints.
 *
 * The existing {@code UserService} tracks failed-login counts <em>per email</em>
 * but does nothing to slow down an attacker who rotates email addresses
 * (e.g. a credential stuffing attack against the registration endpoint).
 * This filter caps the request rate against {@code /api/users/login} and
 * {@code /api/users/register} at a configurable per-IP rate.
 *
 * <p>Limitations:
 * <ul>
 *   <li>In-memory only — does not survive restarts and does not coordinate
 *       across multiple replicas. For multi-instance deployments, swap the
 *       {@link ConcurrentHashMap} for Redis (Bucket4j + Lettuce) or a
 *       managed gateway.</li>
 *   <li>Trusts the inbound {@code X-Forwarded-For} header when set; deploy
 *       behind a reverse proxy that strips client-supplied values to avoid
 *       spoofing.</li>
 * </ul>
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 20;          // burst size
    private static final long REFILL_INTERVAL_MS = 60_000; // 1 minute window
    private static final int REFILL_AMOUNT = 20;     // tokens added per window

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!shouldRateLimit(request)) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for {} on {}", key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please slow down.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/users/login") || uri.startsWith("/api/users/register");
    }

    private String clientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Take only the leftmost IP (the original client).
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    /** Simple token bucket. */
    private static final class Bucket {
        private final AtomicLong tokens = new AtomicLong(CAPACITY);
        private volatile long lastRefill = System.currentTimeMillis();

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - lastRefill >= REFILL_INTERVAL_MS) {
                tokens.set(Math.min(CAPACITY, REFILL_AMOUNT));
                lastRefill = now;
            }
            long current = tokens.get();
            if (current <= 0) return false;
            tokens.decrementAndGet();
            return true;
        }
    }
}
