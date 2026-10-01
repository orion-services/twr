/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.adapter.out.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Specialist that practices textual connectives with one student conversation.
 *
 * @author Rodrigo Prestes Machado
 */
@RegisterAiService
@ApplicationScoped
public interface ConnectiveAgent {

    /**
     * Streams a connective-practice reply grounded in optional RAG context.
     *
     * @param memoryId conversation id on the web, phone number on WhatsApp
     * @param context  relevant passages retrieved from the vector store, possibly empty
     * @param prompt   the student message, or an internal request for the next exercise
     * @return a multi that emits the response chunks
     */
    @SystemMessage(fromResource = "/prompts/connectives.md")
    @UserMessage("Contexto: {context}\n\nPergunta: {prompt}")
    Multi<String> answer(@MemoryId String memoryId, String context, String prompt);

}
