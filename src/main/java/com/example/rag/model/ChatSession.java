package com.example.rag.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSession {
    @Id
    private String id;
    private String sessionId;
    private List<ChatMessage> messages;
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();
    }
}