package com.fileweaver.storage;

import java.time.Instant;

public record PresignedUrl(String url, Instant expiresAt) {}
