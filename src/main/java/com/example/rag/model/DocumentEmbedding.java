package com.example.rag.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "document_embeddings")
public class DocumentEmbedding {
    @Id
    private String id;
    private String documentId;
    private String title;           // top-level for easy querying
    private String content;
    private List<Double> embedding; // 384 dims from all-MiniLM-L6-v2
    private String source;
    private String url;
    private Map<String, Object> metadata;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}