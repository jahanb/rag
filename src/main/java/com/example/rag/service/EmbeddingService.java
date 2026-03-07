package com.example.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real semantic embeddings via Ollama (local, free, no API key).
 * Model: nomic-embed-text — 768 dimensions.
 *
 * Setup:
 *   1. Download Ollama: https://ollama.com/download
 *   2. Run once: ollama pull nomic-embed-text
 *   3. Ollama runs automatically in background — nothing else needed.
 */
@Slf4j
@Service
public class EmbeddingService {

    @Value("${app.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Cannot embed empty text");
        }

        String input = text.length() > 4000 ? text.substring(0, 4000) : text;

        try {
            String response = webClient.post()
                    .uri(ollamaBaseUrl + "/api/embeddings")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("model", embeddingModel, "prompt", input))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode embedding = root.path("embedding");

            List<Double> result = new ArrayList<>();
            for (JsonNode val : embedding) {
                result.add(val.asDouble());
            }

            log.debug("Ollama embedding: {} dims for '{}...'",
                    result.size(), input.substring(0, Math.min(50, input.length())));
            return result;

        } catch (Exception e) {
            log.error("Ollama embedding failed: {}", e.getMessage());
            log.error("Fix: make sure Ollama is running (ollama serve) and model is pulled (ollama pull nomic-embed-text)");
            throw new RuntimeException("Embedding failed — is Ollama running? " + e.getMessage(), e);
        }
    }
}