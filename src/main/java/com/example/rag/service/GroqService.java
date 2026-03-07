package com.example.rag.service;

import com.example.rag.config.AppProperties;
import com.example.rag.dto.RagDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Groq API service — OpenAI-compatible chat completions.
 * Free tier: 14,400 requests/day, 30 req/min for llama-3.3-70b-versatile.
 * Get API key: https://console.groq.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroqService {

    private final AppProperties props;
    private final WebClient groqWebClient;

    /**
     * Generate an answer using Groq's LLM.
     * Exactly ONE API call per invocation — no retries on quota errors.
     */
    public String generateAnswer(String systemPrompt, List<GroqMessage> history) {
        AppProperties.Groq cfg = props.getGroq();

        // Build messages: system prompt + conversation history
        List<GroqMessage> messages = new java.util.ArrayList<>();
        messages.add(new GroqMessage("system", systemPrompt));
        messages.addAll(history);

        GroqRequest request = GroqRequest.builder()
                .model(cfg.getModel())
                .messages(messages)
                .temperature(cfg.getTemperature())
                .max_tokens(cfg.getMaxTokens())
                .build();

        log.debug("Groq request → model: {}, messages: {}", cfg.getModel(), messages.size());

        try {
            GroqResponse response = groqWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .block();

            if (response != null
                    && response.getChoices() != null
                    && !response.getChoices().isEmpty()) {
                String answer = response.getChoices().get(0).getMessage().getContent();
                log.debug("Groq response: {} chars", answer.length());
                return answer;
            }
            throw new RuntimeException("Empty response from Groq API");

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("Groq API HTTP {}: {}", e.getStatusCode(), body);

            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException(
                        "Groq rate limit hit. Please wait a moment and try again. Details: " + body, e);
            }
            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException(
                        "Invalid Groq API key. Check GROQ_API_KEY environment variable.", e);
            }
            throw new RuntimeException("Groq API error: " + body, e);
        } catch (Exception e) {
            log.error("Groq generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Answer generation failed: " + e.getMessage(), e);
        }
    }
}