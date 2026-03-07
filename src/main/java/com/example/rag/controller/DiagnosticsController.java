package com.example.rag.controller;

import com.example.rag.config.AppProperties;
import com.example.rag.repository.DocumentEmbeddingRepository;
import com.example.rag.repository.ChatSessionRepository;
import com.example.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final AppProperties props;
    private final MongoTemplate mongoTemplate;
    private final DocumentEmbeddingRepository embeddingRepo;
    private final ChatSessionRepository sessionRepo;
    private final EmbeddingService embeddingService;

    /**
     * GET /api/diagnostics         — safe, no API calls
     * GET /api/diagnostics?testGroq=true — tests Groq (1 API call)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> diagnose(
            @RequestParam(defaultValue = "false") boolean testGroq) {

        Map<String, Object> report = new LinkedHashMap<>();

        // ── Config ──────────────────────────────────────────────
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("groq_model",         props.getGroq().getModel());
        config.put("groq_api_key_set",   isSet(props.getGroq().getApiKey()));
        config.put("groq_api_key",       preview(props.getGroq().getApiKey()));
        config.put("embedding_model",    props.getEmbedding().getModelName());
        config.put("embedding_dims",     props.getEmbedding().getDimensions());
        config.put("vector_index",       props.getMongodb().getVector().getIndexName());
        config.put("atlas_dims",         props.getMongodb().getVector().getDimensions());
        report.put("configuration", config);

        // ── MongoDB ─────────────────────────────────────────────
        Map<String, Object> mongo = new LinkedHashMap<>();
        try {
            mongo.put("status",          "✅ CONNECTED");
            mongo.put("database",        mongoTemplate.getDb().getName());
            mongo.put("document_chunks", embeddingRepo.count());
            mongo.put("chat_sessions",   sessionRepo.count());
            mongo.put("note", embeddingRepo.count() == 0
                    ? "⚠️ No documents yet — POST /api/documents/ingest to add some"
                    : "✅ Ready for vector search");
        } catch (Exception e) {
            mongo.put("status", "❌ FAILED: " + e.getMessage());
            mongo.put("fix",    "Check MONGODB_URI and Atlas IP whitelist (add 0.0.0.0/0)");
        }
        report.put("mongodb", mongo);

        // ── Local Embeddings ────────────────────────────────────
        Map<String, Object> embedding = new LinkedHashMap<>();
        try {
            long start = System.currentTimeMillis();
            var vec = embeddingService.embed("test sentence");
            long ms  = System.currentTimeMillis() - start;
            embedding.put("status",     "✅ WORKING");
            embedding.put("dimensions", vec.size());
            embedding.put("latency_ms", ms);
            embedding.put("note",       "Local CPU — no API quota used ever");
            embedding.put("match_atlas", vec.size() == props.getMongodb().getVector().getDimensions()
                    ? "✅ Matches Atlas index (" + vec.size() + " dims)"
                    : "❌ MISMATCH! Atlas expects " + props.getMongodb().getVector().getDimensions()
                    + " but got " + vec.size());
        } catch (Exception e) {
            embedding.put("status", "❌ FAILED: " + e.getMessage());
        }
        report.put("local_embedding", embedding);

        // ── Groq API ────────────────────────────────────────────
        if (testGroq) {
            Map<String, Object> groq = new LinkedHashMap<>();
            try {
                var body = Map.of(
                        "model", props.getGroq().getModel(),
                        "messages", java.util.List.of(Map.of("role","user","content","Reply with: OK")),
                        "max_tokens", 10
                );
                var response = WebClient.builder().build().post()
                        .uri(props.getGroq().getBaseUrl() + "/chat/completions")
                        .header("Authorization", "Bearer " + props.getGroq().getApiKey())
                        .header("Content-Type", "application/json")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                var choices = (java.util.List<?>) response.get("choices");
                var msg = (Map<?,?>) ((Map<?,?>) choices.get(0)).get("message");
                groq.put("status",   "✅ WORKING");
                groq.put("model",    props.getGroq().getModel());
                groq.put("response", msg.get("content").toString().trim());
            } catch (Exception e) {
                groq.put("status", "❌ FAILED: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("401"))
                    groq.put("fix", "Invalid GROQ_API_KEY — get one free at https://console.groq.com");
            }
            report.put("groq_api", groq);
        } else {
            report.put("groq_api", Map.of(
                    "status", "⏭️ Not tested",
                    "note",   "Add ?testGroq=true to test (costs 1 Groq API call)"
            ));
        }

        // ── Summary ─────────────────────────────────────────────
        boolean mongoOk = report.get("mongodb").toString().contains("CONNECTED");
        boolean embedOk = report.get("local_embedding").toString().contains("WORKING");
        boolean keySet  = isSet(props.getGroq().getApiKey());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mongodb",           mongoOk ? "✅" : "❌");
        summary.put("local_embeddings",  embedOk ? "✅ (no quota)" : "❌");
        summary.put("groq_key_set",      keySet  ? "✅" : "❌");
        summary.put("ready_to_chat",     mongoOk && embedOk && keySet ? "✅ YES" : "❌ Fix issues above");
        summary.put("groq_free_limits",  "14,400 req/day · 30 req/min — very generous");
        report.put("summary", summary);

        return ResponseEntity.ok(report);
    }

    private boolean isSet(String v) {
        return v != null && !v.isBlank() && !v.startsWith("${");
    }
    private String preview(String v) {
        if (!isSet(v)) return "(not set)";
        return v.length() > 8 ? v.substring(0, 6) + "..." + v.substring(v.length() - 4) : "****";
    }
}