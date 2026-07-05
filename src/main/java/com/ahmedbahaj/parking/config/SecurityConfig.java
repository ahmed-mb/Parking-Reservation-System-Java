package com.ahmedbahaj.parking.config;

import com.ahmedbahaj.parking.security.JwtAuthenticationFilter;
import com.ahmedbahaj.parking.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> {
                // Public API endpoints
                auth.requestMatchers("/api/users/login", "/api/users/register").permitAll();
                auth.requestMatchers("/api/parking/available/**", "/api/parking/available/count").permitAll();
                auth.requestMatchers("/api/config").permitAll();
                
                // Actuator health endpoint (required for Railway healthcheck)
                auth.requestMatchers("/actuator/health").permitAll();
                
                // Static resources (React SPA served by Spring Boot)
                auth.requestMatchers(
                    "/", "/index.html", "/loading",
                    "/assets/**", "/images/**",
                    "/login", "/register", "/dashboard", "/history",
                    "/profile", "/current-booking", "/admin",
                    "/favicon.ico"
                ).permitAll();
                
                // H2 Console - only if explicitly enabled (development only)
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                
                // Admin-only endpoints
                auth.requestMatchers("/api/admin/**").hasAuthority("Admin");
                
                // All other requests require authentication
                auth.anyRequest().authenticated();
            })
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> {
                // Only allow frame options for H2 console if enabled
                if (h2ConsoleEnabled) {
                    headers.frameOptions(frame -> frame.sameOrigin());
                } else {
                    headers.frameOptions(frame -> frame.deny());
                }
                // Force HTTPS for one year, including subdomains. Safe even when
                // running behind Railway/Nginx because the proxy strips the header
                // for HTTP-only access.
                headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000L));
                // Defence-in-depth: deny MIME sniffing and referrer leakage.
                headers.contentTypeOptions(contentTypeOptions -> {});
                headers.referrerPolicy(referrer -> referrer
                    .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                // Restrict permissions and isolate the page from cross-origin embeds.
                headers.crossOriginOpenerPolicy(coop -> coop.policy(
                    org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN));
                headers.crossOriginResourcePolicy(corp -> corp.policy(
                    org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_SITE));
                // CSP tuned to what index.html and the reCAPTCHA v3 widget
                // actually load: Bootstrap/jQuery from jsdelivr + code.jquery.com,
                // Font Awesome (CSS + webfonts) from cdnjs, and Google's
                // recaptcha script/frame/xhr endpoints. 'unsafe-inline' is
                // scoped to style-src only, for the inline style="" attributes
                // in the (now-sanitized) alert/demo-guide HTML strings — no
                // script-src exception is granted.
                headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; "
                    + "script-src 'self' https://cdn.jsdelivr.net https://code.jquery.com https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; "
                    + "style-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com 'unsafe-inline'; "
                    + "font-src 'self' https://cdnjs.cloudflare.com; "
                    + "img-src 'self' data:; "
                    + "connect-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; "
                    + "frame-src https://www.google.com/recaptcha/; "
                    + "frame-ancestors 'none'; "
                    + "base-uri 'self'; "
                    + "form-action 'self'"));
            })
            // Rate-limit auth endpoints first, then validate any Bearer JWT.
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from configuration
        if ("*".equals(allowedOrigins.trim())) {
            // For demo/Docker mode: allow all origins (credentials must be false)
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            configuration.setAllowedOrigins(origins);
            configuration.setAllowCredentials(true);
        }
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Security: Explicitly list allowed headers instead of wildcard
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));
        
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * BCrypt password encoder with cost factor 12.
     * <p>
     * Cost 12 (~250 ms per hash on modern hardware) is the OWASP-recommended
     * minimum for new applications. The default constructor uses cost 10, which
     * is too fast for offline brute-force resistance. Existing hashes remain
     * verifiable because BCrypt embeds the cost in the stored hash.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
