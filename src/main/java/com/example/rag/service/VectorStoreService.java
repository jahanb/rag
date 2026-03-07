package com.example.rag.service;

import com.example.rag.config.AppProperties;
import com.example.rag.dto.RagDtos.SourceDocument;
import com.example.rag.model.DocumentEmbedding;
import com.example.rag.repository.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final DocumentEmbeddingRepository repository;
    private final MongoTemplate mongoTemplate;
    private final EmbeddingService embeddingService;
    private final AppProperties props;

    /** Embed and store a chunk — embedding is generated locally (no API) */
    public void store(String content, String title, String source, String url) {
        try {
            List<Double> embedding = embeddingService.embed(content);
            log.debug("Generated embedding: {} dims for '{}'", embedding.size(), title);

            DocumentEmbedding doc = DocumentEmbedding.builder()
                    .documentId(UUID.randomUUID().toString())
                    .title(title)
                    .content(content)
                    .embedding(embedding)
                    .source(source)
                    .url(url != null ? url : "")
                    .build();

            repository.save(doc);
            log.debug("Saved to MongoDB: '{}' ({} chars, {} dims)", title, content.length(), embedding.size());

        } catch (Exception e) {
            log.error("Failed to store chunk '{}': {}", title, e.getMessage(), e);
            throw new RuntimeException("Store failed for: " + title, e);
        }
    }

    /** MongoDB Atlas $vectorSearch */
    public List<SourceDocument> similaritySearch(String query, int topK) {
        List<Double> queryEmbedding = embeddingService.embed(query);
        AppProperties.Mongodb.Vector cfg = props.getMongodb().getVector();

        log.debug("Vector search: query='{}', topK={}, index='{}'", query, topK, cfg.getIndexName());

        Document vectorSearch = new Document("$vectorSearch", new Document()
                .append("index", cfg.getIndexName())
                .append("path", "embedding")
                .append("queryVector", queryEmbedding)
                .append("numCandidates", cfg.getNumCandidates())
                .append("limit", topK));

        Document addScore = new Document("$addFields",
                new Document("score", new Document("$meta", "vectorSearchScore")));

        try {
            List<Document> results = mongoTemplate.getDb()
                    .getCollection(cfg.getCollection())
                    .aggregate(List.of(vectorSearch, addScore))
                    .into(new ArrayList<>());

            log.info("Vector search returned {} results for query: '{}'", results.size(), query);

            return results.stream().map(doc -> SourceDocument.builder()
                    .content(doc.getString("content"))
                    .title(doc.getString("title") != null ? doc.getString("title") : "Unknown")
                    .source(doc.getString("source"))
                    .url(doc.getString("url") != null ? doc.getString("url") : "")
                    .score(doc.getDouble("score") != null ? doc.getDouble("score") : 0.0)
                    .build()
            ).toList();

        } catch (Exception e) {
            log.error("Vector search FAILED: {}", e.getMessage(), e);
            return List.of();
        }
    }
}