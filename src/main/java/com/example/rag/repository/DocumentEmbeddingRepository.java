package com.example.rag.repository;

import com.example.rag.model.DocumentEmbedding;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface DocumentEmbeddingRepository extends MongoRepository<DocumentEmbedding, String> {
    void deleteByDocumentId(String documentId);
    long countBySource(String source);
}