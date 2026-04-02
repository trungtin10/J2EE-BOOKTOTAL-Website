package com.bookstore.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Load dotenv style file (KEY=VALUE) into Spring Environment for local dev.
 * This is needed because Spring's config import expects .properties/.yml format, not dotenv.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Don't override real environment variables / system properties.
        Map<String, Object> props = new LinkedHashMap<>();

        for (Path p : candidateDotenvFiles()) {
            if (p == null || !Files.exists(p)) continue;
            readDotenvInto(environment, props, p);
        }

        if (!props.isEmpty()) {
            // High precedence but below system properties/env vars (we check before put anyway)
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", props));
        }
    }

    private static void readDotenvInto(ConfigurableEnvironment env, Map<String, Object> out, Path file) {
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String key = s.substring(0, eq).trim();
                String value = s.substring(eq + 1).trim();
                if (!StringUtils.hasText(key)) continue;
                value = stripOptionalQuotes(value);

                // If already configured by env/system/other config, do not override.
                if (env.getProperty(key) != null) continue;
                out.put(key, value);
            }
        } catch (IOException ignored) {
            // optional
        }
    }

    private static String stripOptionalQuotes(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() >= 2) {
            char a = s.charAt(0);
            char b = s.charAt(s.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private static Path[] candidateDotenvFiles() {
        // When running from springboot-backend/, user keeps .env at repo root.
        // Also support keeping a local .env inside springboot-backend/.
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        return new Path[] {
                cwd.resolve(".env"),
                cwd.resolve("..").resolve(".env").normalize()
        };
    }

    @Override
    public int getOrder() {
        // Run early
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

