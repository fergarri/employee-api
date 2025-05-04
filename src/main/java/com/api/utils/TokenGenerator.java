package com.api.utils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

public class TokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera un token aleatorio seguro
     * @return String token
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Calcula el timestamp de expiración basado en los minutos especificados
     * @param minutes minutos hasta la expiración
     * @return timestamp de expiración en segundos desde epoch
     */
    public static long calculateExpirationTime(int minutes) {
        return Instant.now().getEpochSecond() + (minutes * 60);
    }
}