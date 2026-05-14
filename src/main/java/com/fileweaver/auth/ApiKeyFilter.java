package com.fileweaver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileweaver.api.dto.ApiError;
import jakarta.servlet.FilterChain;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";
    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final ApiKeyService service;
    private final ObjectMapper mapper;

    @Value("${app.auth.admin-key:}")
    private String adminKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // public endpoints
        return path.equals("/health")
            || path.startsWith("/actuator")
            || path.startsWith("/admin");   // /admin/* uses AdminKeyFilter instead
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);

        // permit /admin/* keys to also be used as app keys (admin-elevated)
        if ((header == null || header.isBlank()) && adminKey != null && !adminKey.isBlank()) {
            String adminHeader = request.getHeader(ADMIN_HEADER);
            if (adminKey.equals(adminHeader)) {
                request.setAttribute(AuthenticatedKeyResolver.REQUEST_ATTR, syntheticAdminKey());
                chain.doFilter(request, response);
                return;
            }
        }

        Optional<ApiKey> key = service.validate(header);
        if (key.isEmpty()) {
            unauthorized(response);
            return;
        }
        request.setAttribute(AuthenticatedKeyResolver.REQUEST_ATTR, key.get());
        service.touchLastUsed(key.get());
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
            new ApiError("UNAUTHORIZED", "Missing or invalid API key"));
    }

    private static ApiKey syntheticAdminKey() {
        return ApiKey.builder()
            .id("__admin__")
            .name("master-admin")
            .active(true)
            .scopes(java.util.List.of("admin", "reports:create", "reports:read"))
            .build();
    }
}
