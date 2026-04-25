package com.fileweaver.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CreateApiKeyRequest {

    @NotBlank
    private String name;

    private List<String> scopes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }
}
