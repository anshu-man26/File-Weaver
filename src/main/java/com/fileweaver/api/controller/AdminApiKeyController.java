package com.fileweaver.api.controller;

import com.fileweaver.api.dto.ApiKeyResponse;
import com.fileweaver.api.dto.CreateApiKeyRequest;
import com.fileweaver.auth.ApiKey;
import com.fileweaver.auth.ApiKeyGenerator;
import com.fileweaver.auth.ApiKeyRepository;
import com.fileweaver.auth.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/api-keys")
@RequiredArgsConstructor
public class AdminApiKeyController {

    private final ApiKeyRepository repo;
    private final ApiKeyService service;

    @PostMapping
    public ResponseEntity<ApiKeyResponse> create(@Valid @RequestBody CreateApiKeyRequest req) {
        if (repo.findAll().stream().anyMatch(k -> k.getName().equals(req.getName()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Key name already exists: " + req.getName());
        }
        String plaintext = ApiKeyGenerator.generate();
        ApiKey k = ApiKey.builder()
            .name(req.getName())
            .keyHash(ApiKeyGenerator.hash(plaintext))
            .prefix(ApiKeyGenerator.prefix(plaintext))
            .scopes(req.getScopes() != null ? req.getScopes() : List.of("reports:create", "reports:read"))
            .active(true)
            .createdAt(Instant.now())
            .createdByName("admin")
            .build();
        ApiKey saved = repo.save(k);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiKeyResponse.withPlaintext(saved, plaintext));
    }

    @GetMapping
    public List<ApiKeyResponse> list() {
        return repo.findAll().stream().map(ApiKeyResponse::from).toList();
    }

    @PatchMapping("/{id}/revoke")
    public ApiKeyResponse revoke(@PathVariable String id) {
        ApiKey k = repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Key not found"));
        k.setActive(false);
        repo.save(k);
        service.invalidate(k.getKeyHash());
        return ApiKeyResponse.from(k);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ApiKey k = repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Key not found"));
        repo.deleteById(id);
        service.invalidate(k.getKeyHash());
        return ResponseEntity.noContent().build();
    }
}
