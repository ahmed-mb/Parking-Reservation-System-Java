package com.ahmedbahaj.parking.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Public configuration endpoint.
 *
 * Returns non-sensitive app config for the frontend (demo mode flag,
 * session timeout hint, reCAPTCHA site key). The site key is shipped at
 * runtime instead of being baked into the JavaScript bundle at build
 * time so that key rotation doesn't require a rebuild and so that
 * deployment targets (Railway, Docker hosts) only need to set a single
 * RECAPTCHA_SITE_KEY environment variable rather than threading a Vite
 * build argument through the Dockerfile.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${demo.mode:false}")
    private boolean demoMode;

    @Value("${demo.session.timeout:0}")
    private int sessionTimeout;

    /**
     * reCAPTCHA v3 site key. PUBLIC by design — it ships in the HTML
     * source of every page that loads the reCAPTCHA script, so exposing
     * it via this unauthenticated endpoint is no leak. The SECRET key
     * stays server-side and is only used by RecaptchaService when
     * verifying tokens with Google.
     */
    @Value("${recaptcha.site-key:}")
    private String recaptchaSiteKey;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        // HashMap (not Map.of) because Map.of throws NullPointerException on
        // any null value, and `recaptchaSiteKey` may be the empty string
        // during local dev when no key is configured.
        Map<String, Object> body = new HashMap<>();
        body.put("demoMode", demoMode);
        body.put("sessionTimeout", sessionTimeout);
        body.put("recaptchaSiteKey", recaptchaSiteKey == null ? "" : recaptchaSiteKey);
        return ResponseEntity.ok(body);
    }
}
