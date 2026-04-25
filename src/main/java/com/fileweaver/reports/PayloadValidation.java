package com.fileweaver.reports;

import java.util.Map;

/**
 * Tiny validation helpers for Report.validate(payload). Throws IllegalArgumentException
 * with a readable message — picked up by the API exception handler as 400.
 */
public final class PayloadValidation {

    private PayloadValidation() {}

    public static void require(Map<String, Object> payload, String key, Class<?> expected) {
        Object v = payload.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        if (!expected.isInstance(v)) {
            throw new IllegalArgumentException(
                "Field " + key + " must be " + expected.getSimpleName()
                    + " (got " + v.getClass().getSimpleName() + ")");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String, Object> payload, String key, Class<T> expected) {
        require(payload, key, expected);
        return (T) payload.get(key);
    }
}
