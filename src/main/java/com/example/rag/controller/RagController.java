package com.example.rag.controller;

import com.example.rag.dto.RagDtos.*;
import com.example.rag.service.DocumentIngestionService;
import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final DocumentIngestionService ingestionService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(ragService.chat(request));
    }

    @PostMapping("/documents/ingest")
    public ResponseEntity<IngestResponse> ingest(@RequestBody IngestRequest request) {
        return ResponseEntity.ok(ingestionService.ingest(request));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG system running — Groq + local embeddings");
    }
}