package com.fileweaver.reports.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wraps a secondary MongoDB connection to ClickAndCare's database.
 *
 * The MongoTemplate is held internally — never published as a Spring bean —
 * so Spring Boot's auto-configuration of the *primary* MongoTemplate (used
 * by fileweaver's own jobs/apiKeys repositories) isn't disturbed by
 * @ConditionalOnMissingBean checks.
 *
 * Registered only when app.clickandcare.mongodb-uri is set (env:
 * CLICKANDCARE_MONGODB_URI). When missing, AppointmentLogReport injects
 * null and fails the request at validate() with a clear error.
 */
@Service
@ConditionalOnProperty(name = "app.clickandcare.mongodb-uri")
public class ClickAndCareGateway {

    private final MongoClient client;
    private final MongoTemplate template;

    public ClickAndCareGateway(@Value("${app.clickandcare.mongodb-uri}") String uri) {
        ConnectionString cs = new ConnectionString(uri);
        this.client = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(cs)
            .build());
        // ClickAndCare's backend appends `Click&Care` as the DB name when
        // the URI doesn't include one — match that.
        String dbName = cs.getDatabase() != null && !cs.getDatabase().isBlank()
            ? cs.getDatabase()
            : "Click&Care";
        this.template = new MongoTemplate(client, dbName);
    }

    /** Most-recent-first list of appointment documents for a user. */
    public List<Document> findAppointmentsByUserId(String userId) {
        Query q = Query.query(Criteria.where("userId").is(userId))
            .with(Sort.by(Sort.Direction.DESC, "date"));
        return template.find(q, Document.class, "appointments");
    }

    /** Single appointment by its 24-char hex ObjectId. Null if not found. */
    public Document findAppointmentById(String id) {
        if (id == null || !ObjectId.isValid(id)) return null;
        Query q = Query.query(Criteria.where("_id").is(new ObjectId(id)));
        return template.findOne(q, Document.class, "appointments");
    }

    @PreDestroy
    public void close() {
        if (client != null) client.close();
    }
}
