package com.bookstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OAuthClientDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(OAuthClientDiagnostics.class);

    @Bean
    ApplicationRunner oauthClientDiagnosticsRunner(Environment env) {
        return args -> {
            String clientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
            String secret = env.getProperty("spring.security.oauth2.client.registration.google.client-secret");

            String safeId = maskMiddle(clientId);
            boolean idMissing = clientId == null || clientId.isBlank() || clientId.startsWith("disabled-");
            boolean secretMissing = secret == null || secret.isBlank() || secret.startsWith("disabled-");

            log.info("[OAuth2] Google client-id: {}", safeId);
            log.info("[OAuth2] Google client configured? {}", (!idMissing && !secretMissing));
            if (idMissing || secretMissing) {
                log.warn("[OAuth2] Google OAuth keys missing/disabled. Check .env or environment variables.");
            }
        };
    }

    private static String maskMiddle(String s) {
        if (s == null) return "(null)";
        String t = s.trim();
        if (t.isEmpty()) return "(blank)";
        if (t.length() <= 12) return t;
        return t.substring(0, 6) + "..." + t.substring(t.length() - 6);
    }
}

