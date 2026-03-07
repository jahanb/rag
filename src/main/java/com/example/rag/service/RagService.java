package com.example.rag.service;

import com.example.rag.config.AppProperties;
import com.example.rag.dto.RagDtos.*;
import com.example.rag.model.ChatSession;
import com.example.rag.repository.ChatSessionRepository;
import com.example.rag.repository.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final double MIN_RELEVANCE_SCORE = 0.4; // below this = DB found nothing useful

    private final VectorStoreService vectorStoreService;
    private final GroqService groqService;
    private final ChatSessionRepository chatSessionRepository;
    private final DocumentEmbeddingRepository documentEmbeddingRepository;
    private final AppProperties props;

    public ChatResponse chat(ChatRequest request) {
        log.info("RAG query: {}", request.getQuestion());

        // ── 1. Vector search ───────────────────────────────────
        List<SourceDocument> allSources = new ArrayList<>();
        List<SourceDocument> relevantSources = new ArrayList<>();
        long docCount = documentEmbeddingRepository.count();

        if (docCount > 0) {
            allSources = vectorStoreService.similaritySearch(
                    request.getQuestion(),
                    props.getRag().getMaxContextDocs()
            );
            // Filter out low-relevance chunks
            for (SourceDocument s : allSources) {
                if (s.getScore() >= MIN_RELEVANCE_SCORE) {
                    relevantSources.add(s);
                }
            }
            log.debug("Vector search: {}/{} chunks above relevance threshold {}",
                    relevantSources.size(), allSources.size(), MIN_RELEVANCE_SCORE);
        }

        // ── 2. Determine answer source ─────────────────────────
        double topScore = allSources.isEmpty() ? 0.0 : allSources.get(0).getScore();
        String answerSource;
        String debugInfo;

        if (docCount == 0) {
            answerSource = "groq_only";
            debugInfo = "⚠️ Database is empty — answer is from Groq's training data only. Ingest documents first.";
        } else if (relevantSources.isEmpty()) {
            answerSource = "groq_only";
            debugInfo = String.format(
                    "⚠️ DB searched (%d chunks) but best match score was %.2f (below %.1f threshold) — answer is from Groq's training data only.",
                    docCount, topScore, MIN_RELEVANCE_SCORE);
        } else {
            answerSource = "database";
            debugInfo = String.format(
                    "✅ Answer based on %d chunk(s) from your database. Top match: \"%s\" (score: %.2f).",
                    relevantSources.size(),
                    relevantSources.get(0).getTitle(),
                    topScore);
        }

        log.info("Answer source: {} | top score: {} | relevant chunks: {}",
                answerSource, topScore, relevantSources.size());

        // ── 3. Session history ─────────────────────────────────
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                .orElse(ChatSession.builder()
                        .sessionId(sessionId)
                        .messages(new ArrayList<>())
                        .build());

        // ── 4. Build prompt ────────────────────────────────────
        String systemPrompt = buildSystemPrompt(relevantSources, answerSource);
        List<GroqMessage> history = buildHistory(session, request.getQuestion());

        // ── 5. Single Groq API call ────────────────────────────
        String answer = groqService.generateAnswer(systemPrompt, history);

        // ── 6. Save session ────────────────────────────────────
        session.getMessages().add(ChatSession.ChatMessage.builder()
                .role("user").content(request.getQuestion()).build());
        session.getMessages().add(ChatSession.ChatMessage.builder()
                .role("assistant").content(answer).build());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        return ChatResponse.builder()
                .answer(answer)
                .sources(relevantSources)
                .sessionId(sessionId)
                .answerSource(answerSource)
                .dbChunksFound(relevantSources.size())
                .topMatchScore(Math.round(topScore * 100.0) / 100.0)
                .debugInfo(debugInfo)
                .build();
    }

    private String buildSystemPrompt(List<SourceDocument> sources, String answerSource) {
        if (sources.isEmpty()) {
            return "You are a helpful AI assistant. Answer clearly and concisely from your training knowledge. "
                    + "Do NOT say things like 'based on the provided context' since there is no context — "
                    + "just answer directly.";
        }

        StringBuilder sb = new StringBuilder(
                "You are a helpful AI assistant.\n"
                        + "First try to answer using the context below. "
                        + "If the context does not contain the answer, use your own training knowledge to answer — never say the context is insufficient, just answer the question.\n"
                        + "Do NOT start your answer with 'Based on the provided context' — just answer directly.\n\n"
                        + "=== CONTEXT FROM DATABASE ===\n\n");

        for (int i = 0; i < sources.size(); i++) {
            SourceDocument s = sources.get(i);
            sb.append(String.format("[%d] %s (relevance: %.2f)\n", i + 1, s.getTitle(), s.getScore()));
            sb.append(s.getContent()).append("\n\n");
        }
        sb.append("=== END CONTEXT ===\n");
        return sb.toString();
    }

    private List<GroqMessage> buildHistory(ChatSession session, String currentQuestion) {
        List<GroqMessage> history = new ArrayList<>();
        List<ChatSession.ChatMessage> msgs = session.getMessages();
        int start = Math.max(0, msgs.size() - 6);
        for (int i = start; i < msgs.size(); i++) {
            ChatSession.ChatMessage m = msgs.get(i);
            history.add(new GroqMessage("user".equals(m.getRole()) ? "user" : "assistant", m.getContent()));
        }
        history.add(new GroqMessage("user", currentQuestion));
        return history;
    }
}