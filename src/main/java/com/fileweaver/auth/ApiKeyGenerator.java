package com.fileweaver.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class ApiKeyGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int BODY_LENGTH = 36;
    public static final String PREFIX = "fwk_";

    private ApiKeyGenerator() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX.length() + BODY_LENGTH);
        sb.append(PREFIX);
        for (int i = 0; i < BODY_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String hash(String plaintext) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String prefix(String plaintext) {
        return plaintext.substring(0, Math.min(plaintext.length(), 8));
    }
}
