package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.dto.RecaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies Google reCAPTCHA v3 tokens submitted with login and registration
 * requests, as a bot-mitigation layer in front of the auth endpoints.
 *
 * <p>Verification is always enforced, including in {@code demo.mode} — the
 * demo deployment must have its own real reCAPTCHA site registered for its
 * domain (see {@code .env.example}). A prior version of this class bypassed
 * verification whenever {@code demo.mode=true}, which meant the one publicly
 * reachable deployment had no bot mitigation on login/register at all.
 */
@Slf4j
@Service
public class RecaptchaService {

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final float SCORE_THRESHOLD = 0.5f;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Verifies a reCAPTCHA token against Google's siteverify API.
     *
     * <p>A token is considered valid only if Google reports {@code success} and
     * the v3 risk {@code score} is at least {@value #SCORE_THRESHOLD} (lower
     * scores indicate likely bot traffic). Any network or parsing error is
     * treated as a failed verification ({@code false}) so callers fail closed.
     *
     * @param recaptchaToken the client-supplied reCAPTCHA token; null/empty fails
     * @return {@code true} if the token passes verification
     */
    public boolean verifyRecaptcha(String recaptchaToken) {
        if (recaptchaToken == null || recaptchaToken.isEmpty()) {
            log.warn("reCAPTCHA token is empty");
            return false;
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", recaptchaToken);

        try {
            RecaptchaResponse response = restTemplate.postForObject(
                RECAPTCHA_VERIFY_URL, params, RecaptchaResponse.class
            );

            if (response != null && response.isSuccess()) {
                boolean isValid = response.getScore() >= SCORE_THRESHOLD;
                log.debug("reCAPTCHA v3 score: {}, valid: {}", response.getScore(), isValid);
                return isValid;
            }

            log.warn("reCAPTCHA validation failed: response null or not successful");
            return false;
        } catch (Exception e) {
            log.error("reCAPTCHA verification error: {}", e.getMessage());
            return false;
        }
    }
}
