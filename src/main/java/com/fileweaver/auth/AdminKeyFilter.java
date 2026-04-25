package com.fileweaver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileweaver.api.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Admin-Key";

    private final ObjectMapper mapper;

    @Value("${app.auth.admin-key:}")
    private String adminKey;

    public AdminKeyFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (adminKey == null || adminKey.isBlank()) {
            forbid(response, "Admin key not configured");
            return;
        }
        String provided = request.getHeader(HEADER);
        if (provided == null || !slowEquals(adminKey, provided)) {
            forbid(response, "Missing or invalid admin key");
            return;
        }
        chain.doFilter(request, response);
    }

    private void forbid(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
            new ApiError("UNAUTHORIZED", message));
    }

    /** constant-time comparison */
    private static boolean slowEquals(String a, String b) {
        byte[] x = a.getBytes();
        byte[] y = b.getBytes();
        if (x.length != y.length) return false;
        int diff = 0;
        for (int i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
        return diff == 0;
    }
}
