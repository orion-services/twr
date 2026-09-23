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
 * LangChain4j AI service that answers legal questions for a law office.
 *
 * @author Rodrigo Prestes Machado
 */
@RegisterAiService
@ApplicationScoped
public interface DoraAgent {

    /**
     * Streams a chat response grounded in RAG-retrieved context.
     *
     * @param memoryId stable identifier (phone number) used to isolate conversational memory per user
     * @param context  relevant passages retrieved from the vector store (may be empty)
     * @param prompt   the user question
     * @return a multi that emits the response chunks
     */
    @SystemMessage("""
        Você é um assistente virtual.
        Você deve responder de forma amigável, mas profissional, e fornecer
        informações precisas e relevantes.
        Utilize o contexto abaixo, quando relevante, para fundamentar sua resposta.
        Se o contexto não for suficiente ou não estiver relacionado à pergunta,
        responda com seu próprio conhecimento, deixando claro quando a informação
        não vier do contexto fornecido.
    """)
    @UserMessage("Contexto: {context}\n\nPergunta: {prompt}")
    Multi<String> answer(@MemoryId String memoryId, String context, String prompt);

}
