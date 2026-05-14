package com.fileweaver.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @NoArgsConstructor
public class CreateApiKeyRequest {

    @NotBlank private String       name;
             private List<String>  scopes;
}
