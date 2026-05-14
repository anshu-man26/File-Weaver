package com.fileweaver.reports.clickandcare;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnExpression("'${app.clickandcare.mongodb-uri:}'.startsWith('mongodb')")
public class ClickAndCareGateway {

    private final MongoClient client;
    private final MongoTemplate template;

    public ClickAndCareGateway(@Value("${app.clickandcare.mongodb-uri}") String uri) {
        ConnectionString cs = new ConnectionString(uri);
        this.client = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(cs)
            .build());
        String dbName = cs.getDatabase() != null && !cs.getDatabase().isBlank()
            ? cs.getDatabase()
            : "Click&Care";
        this.template = new MongoTemplate(client, dbName);
    }

    public List<Document> findAppointmentsByUserId(String userId) {
        Query q = Query.query(Criteria.where("userId").is(userId))
            .with(Sort.by(Sort.Direction.DESC, "date"));
        return template.find(q, Document.class, "appointments");
    }

    public Document findAppointmentById(String id) {
        if (id == null || !ObjectId.isValid(id)) return null;
        Query q = Query.query(Criteria.where("_id").is(new ObjectId(id)));
        return template.findOne(q, Document.class, "appointments");
    }

    @PreDestroy
    public void close() {
        if (client != null) client.close();
    }

    public static String field(Document d, String key, String fallback) {
        if (d == null) return fallback;
        String v = d.getString(key);
        return v == null ? fallback : v;
    }
}
