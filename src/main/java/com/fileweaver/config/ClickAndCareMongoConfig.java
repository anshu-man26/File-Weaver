package com.fileweaver.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Secondary MongoDB connection pointing at ClickAndCare's database.
 *
 * Only registered when app.clickandcare.mongodb-uri (env: CLICKANDCARE_MONGODB_URI)
 * is set. autowireCandidate = false keeps Spring from trying to inject this
 * MongoTemplate into beans that ask for the primary one (HealthController,
 * Spring Data repositories, etc.) — only explicit @Qualifier lookups will find it.
 *
 * Used by AppointmentLogReport to read the appointments collection straight
 * from ClickAndCare's DB rather than receiving the data in the request payload.
 */
@Configuration
@ConditionalOnProperty(name = "app.clickandcare.mongodb-uri")
public class ClickAndCareMongoConfig {

    @Bean(name = "clickandcareMongoTemplate", autowireCandidate = false)
    public MongoTemplate clickandcareMongoTemplate(
            @Value("${app.clickandcare.mongodb-uri}") String uri) {
        ConnectionString cs = new ConnectionString(uri);
        MongoClient client = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(cs)
            .build());
        // Use the database name encoded in the URI; default to "Click&Care"
        // (ClickAndCare's backend always appends that name in config/mongodb.js).
        String dbName = cs.getDatabase() != null && !cs.getDatabase().isBlank()
            ? cs.getDatabase()
            : "Click&Care";
        return new MongoTemplate(client, dbName);
    }
}
