package com.fileweaver.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "apiKeys")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiKey {

    @Id                     private String       id;
    @Indexed(unique = true) private String       name;
    @Indexed(unique = true) private String       keyHash;
    @Indexed                private String       prefix;

    private List<String>    scopes;
    private boolean         active;
    private Instant         lastUsedAt;
    private Instant         createdAt;
    private String          createdByName;
}
