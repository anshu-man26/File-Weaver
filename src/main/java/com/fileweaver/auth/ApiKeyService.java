package com.fileweaver.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Validates API keys with two-layer caching:
 *  - positive cache (TTL: app.auth.cache-ttl, default 5m)
 *  - negative cache (30s) to absorb floods of invalid keys
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyRepository repo;

    private final Cache<String, ApiKey> positive;
    private final Cache<String, Boolean> negative;

    public ApiKeyService(ApiKeyRepository repo,
                         @Value("${app.auth.cache-ttl:PT5M}") Duration ttl) {
        this.repo = repo;
        this.positive = Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(10_000)
            .build();
        this.negative = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(10_000)
            .build();
    }

    public Optional<ApiKey> validate(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return Optional.empty();
        String hash = ApiKeyGenerator.hash(plaintext);

        ApiKey hit = positive.getIfPresent(hash);
        if (hit != null) return Optional.of(hit);

        if (Boolean.TRUE.equals(negative.getIfPresent(hash))) {
            return Optional.empty();
        }

        Optional<ApiKey> found = repo.findByKeyHash(hash);
        if (found.isEmpty() || !found.get().isActive()) {
            negative.put(hash, Boolean.TRUE);
            return Optional.empty();
        }
        positive.put(hash, found.get());
        return found;
    }

    @Async
    public void touchLastUsed(ApiKey key) {
        try {
            key.setLastUsedAt(Instant.now());
            repo.save(key);
        } catch (Exception e) {
            log.warn("Failed to update lastUsedAt for key {}", key.getId(), e);
        }
    }

    public void invalidate(String keyHash) {
        positive.invalidate(keyHash);
        negative.invalidate(keyHash);
    }

    public void invalidateAll() {
        positive.invalidateAll();
        negative.invalidateAll();
    }
}
