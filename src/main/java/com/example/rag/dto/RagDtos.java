package com.example.rag.dto;

import lombok.*;
import java.util.List;

public class RagDtos {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ChatRequest {
        private String question;
        private String sessionId;
        private boolean useWebSearch = false;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChatResponse {
        private String answer;
        private List<SourceDocument> sources;
        private String sessionId;

        // ── Transparency fields ──────────────────────────────
        private String answerSource;      // "database", "groq_only", or "database+groq"
        private int    dbChunksFound;     // how many DB chunks were used as context
        private double topMatchScore;     // similarity score of best chunk (0.0 = no match)
        private String debugInfo;         // human-readable summary for the UI
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class IngestRequest {
        private String content;
        private String title;
        private String source;
        private String url;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngestResponse {
        private String documentId;
        private int chunksCreated;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SourceDocument {
        private String content;
        private String title;
        private String source;
        private String url;
        private double score;
    }

    // ── Groq API DTOs (OpenAI-compatible format) ──────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GroqRequest {
        private String model;
        private List<GroqMessage> messages;
        private double temperature;
        private int max_tokens;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GroqMessage {
        private String role;
        private String content;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GroqResponse {
        private List<Choice> choices;

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class Choice {
            private GroqMessage message;
        }
    }
}