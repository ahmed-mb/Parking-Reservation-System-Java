package com.ahmedbahaj.parking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitingFilter}. The bucket size is fixed at 20
 * tokens / minute, so we expect the 21st request from the same IP within the
 * window to be rejected with HTTP 429.
 */
class RateLimitingFilterTest {

    @Test
    @DisplayName("non-auth endpoints are not rate-limited")
    void nonAuthEndpoint_passesThrough() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest req = buildRequest("/api/parking/available", "1.2.3.4");
        HttpServletResponse resp = new MockHttpServletResponse();

        for (int i = 0; i < 100; i++) {
            filter.doFilter(req, resp, chain);
        }
        verify(chain, times(100)).doFilter(any(), any());
    }

    @Test
    @DisplayName("auth endpoint blocks the 21st request from a single IP within 1 minute")
    void authEndpoint_isRateLimited() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest req = buildRequest("/api/users/login", "5.6.7.8");

        // First 20 requests should pass.
        for (int i = 0; i < 20; i++) {
            HttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, chain);
            assertEquals(200, resp.getStatus(), "request " + (i + 1) + " should not have been blocked");
        }

        // 21st request should be rejected with 429.
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(req, blocked, chain);
        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        // The chain should not have been invoked for the blocked request.
        verify(chain, times(20)).doFilter(any(), any());
    }

    @Test
    @DisplayName("different IPs get independent buckets")
    void differentIps_haveIndependentBuckets() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        FilterChain chain = mock(FilterChain.class);

        // Drain bucket for IP A.
        for (int i = 0; i < 21; i++) {
            filter.doFilter(buildRequest("/api/users/login", "10.0.0.1"),
                            new MockHttpServletResponse(), chain);
        }

        // IP B should still be allowed.
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(buildRequest("/api/users/login", "10.0.0.2"), resp, chain);
        assertEquals(200, resp.getStatus());
    }

    @Test
    @DisplayName("X-Forwarded-For is preferred over remote addr for the bucket key")
    void xForwardedFor_isUsedAsClientKey() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        FilterChain chain = mock(FilterChain.class);

        // 20 requests through proxy with the same XFF header should drain
        // the same bucket regardless of remote-addr noise.
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = (MockHttpServletRequest) buildRequest("/api/users/login", "127.0.0.1");
            req.addHeader("X-Forwarded-For", "9.9.9.9, 192.168.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }
        MockHttpServletRequest req = (MockHttpServletRequest) buildRequest("/api/users/login", "127.0.0.1");
        req.addHeader("X-Forwarded-For", "9.9.9.9, 192.168.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, chain);
        assertEquals(429, resp.getStatus());
    }

    private static HttpServletRequest buildRequest(String uri, String remote) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(uri);
        req.setRemoteAddr(remote);
        return req;
    }
}
