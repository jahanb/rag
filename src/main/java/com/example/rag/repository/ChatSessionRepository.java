package com.example.rag.repository;



import com.example.rag.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    Optional<ChatSession> findBySessionId(String sessionId);
}