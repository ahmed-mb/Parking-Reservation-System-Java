package com.ahmedbahaj.parking.service;

import com.ahmedbahaj.parking.dto.RecaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class RecaptchaService {

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${demo.mode:false}")
    private boolean demoMode;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final float SCORE_THRESHOLD = 0.5f;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyRecaptcha(String recaptchaToken) {
        // In demo mode, bypass reCAPTCHA verification entirely
        if (demoMode) {
            log.info("Demo mode: reCAPTCHA verification bypassed");
            return true;
        }

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
