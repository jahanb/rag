package com.example.rag.service;

import com.example.rag.config.AppProperties;
import com.example.rag.dto.RagDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStoreService vectorStoreService;
    private final AppProperties props;

    public IngestResponse ingest(IngestRequest request) {
        String title  = request.getTitle()  != null ? request.getTitle()  : "Untitled";
        String source = request.getSource() != null ? request.getSource() : "manual";
        String url    = request.getUrl()    != null ? request.getUrl()    : "";

        List<String> chunks = chunkText(
                request.getContent(),
                props.getRag().getChunkSize(),
                props.getRag().getChunkOverlap()
        );

        log.info("Ingesting '{}': {} chunks to process", title, chunks.size());

        int stored = 0;
        for (int i = 0; i < chunks.size(); i++) {
            try {
                // For multi-chunk docs append index to title so each chunk is identifiable
                String chunkTitle = chunks.size() > 1
                        ? title + " (part " + (i + 1) + "/" + chunks.size() + ")"
                        : title;

                vectorStoreService.store(chunks.get(i), chunkTitle, source, url);
                stored++;
                log.debug("Chunk {}/{} stored for '{}'", i + 1, chunks.size(), title);
            } catch (Exception e) {
                log.error("Chunk {}/{} FAILED for '{}': {}", i + 1, chunks.size(), title, e.getMessage());
            }
        }

        String message = stored == chunks.size()
                ? String.format("✅ Ingested '%s': %d chunk(s) stored with embeddings", title, stored)
                : String.format("⚠️ Partial ingest '%s': %d/%d chunks stored", title, stored, chunks.size());

        log.info(message);

        return IngestResponse.builder()
                .documentId(UUID.randomUUID().toString())
                .chunksCreated(stored)
                .message(message)
                .build();
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        text = text.replaceAll("\\s+", " ").trim();

        // If the whole text fits in one chunk, just return it
        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at a sentence boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf(". ", end);
                if (lastPeriod > start + chunkSize / 2) end = lastPeriod + 1;
            }

            chunks.add(text.substring(start, end).trim());

            // Advance start, ensuring it always moves forward
            int next = end - overlap;
            start = Math.max(next, start + 1); // never go backwards

            if (start >= text.length()) break;
        }
        return chunks;
    }
}