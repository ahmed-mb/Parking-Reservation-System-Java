package com.ahmedbahaj.parking.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public configuration endpoint.
 * Returns non-sensitive app config for the frontend (e.g., demo mode status).
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${demo.mode:false}")
    private boolean demoMode;

    @Value("${demo.session.timeout:0}")
    private int sessionTimeout;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "demoMode", demoMode,
            "sessionTimeout", sessionTimeout
        ));
    }
}
