package com.staylanka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "staylanka")
public record StayLankaProperties(Uploads uploads, Seed seed) {
    public record Uploads(String directory, long maxBytes) {
    }

    public record Seed(boolean enabled, String adminEmail, String adminPassword,
                       String staffEmail, String staffPassword) {
    }
}

