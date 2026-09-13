package com.staylanka.common;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ReferenceGenerator {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private final SecureRandom random = new SecureRandom();

    public String next(String prefix) {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return prefix + "-" + LocalDate.now().format(DATE) + "-" + suffix;
    }
}

