package com.fileweaver.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyGeneratorTest {

    @Test
    void generates_unique_keys_with_correct_prefix_and_length() {
        String a = ApiKeyGenerator.generate();
        String b = ApiKeyGenerator.generate();
        assertThat(a).startsWith("fwk_").hasSize(40);
        assertThat(b).startsWith("fwk_").hasSize(40);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hash_is_deterministic_and_hex() {
        String h1 = ApiKeyGenerator.hash("fwk_abc");
        String h2 = ApiKeyGenerator.hash("fwk_abc");
        assertThat(h1).isEqualTo(h2).hasSize(64).matches("^[0-9a-f]{64}$");
    }

    @Test
    void different_plaintexts_hash_differently() {
        assertThat(ApiKeyGenerator.hash("fwk_a")).isNotEqualTo(ApiKeyGenerator.hash("fwk_b"));
    }
}
