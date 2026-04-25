package com.fileweaver.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fileweaver.auth.ApiKey;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiKeyResponse(
    String id,
    String name,
    String prefix,
    String plaintext,
    List<String> scopes,
    boolean active,
    Instant lastUsedAt,
    Instant createdAt,
    String createdByName
) {
    public static ApiKeyResponse withPlaintext(ApiKey k, String plaintext) {
        return new ApiKeyResponse(k.getId(), k.getName(), k.getPrefix(), plaintext,
            k.getScopes(), k.isActive(), k.getLastUsedAt(), k.getCreatedAt(), k.getCreatedByName());
    }

    public static ApiKeyResponse from(ApiKey k) {
        return new ApiKeyResponse(k.getId(), k.getName(), k.getPrefix(), null,
            k.getScopes(), k.isActive(), k.getLastUsedAt(), k.getCreatedAt(), k.getCreatedByName());
    }
}
