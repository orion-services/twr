/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.application;

import java.util.Date;

import dev.rpmhub.adapter.out.ai.ConnectiveAgent;
import dev.rpmhub.adapter.out.ai.ExpansionAgent;
import dev.rpmhub.domain.model.AgentMessage;
import dev.rpmhub.domain.model.Chat;
import dev.rpmhub.domain.model.RagQuery;
import dev.rpmhub.domain.model.RagResponse;
import dev.rpmhub.domain.model.TutorActivity;
import dev.rpmhub.domain.port.out.EmbeddingRepository;
import dev.rpmhub.domain.port.out.Repository;
import io.smallrye.mutiny.Multi;

/**
 * Calls the specialist stored on a conversation and persists the reply.
 *
 * <p>The Redis memory id is chosen by the caller: the conversation id on the web,
 * the phone number on WhatsApp. This class does not select or clear that id.
 *
 * @author Rodrigo Prestes Machado
 */
class SkillReply {

    /** Fallback context string used when no relevant chunk is found. */
    private static final String DEFAULT_CONTEXT = "";

    private final Repository repository;

    private final EmbeddingRepository embeddingRepository;

    private final ConnectiveAgent connectiveAgent;

    private final ExpansionAgent expansionAgent;

    private final int maxResults;

    private final double minScore;

    /**
     * Creates the reply helper.
     *
     * @param repository          port used to persist the agent reply
     * @param embeddingRepository port for vector-similarity search
     * @param connectiveAgent     specialist for connectives
     * @param expansionAgent      specialist for sentence expansion
     * @param maxResults          number of context chunks retrieved per message
     * @param minScore            minimum similarity score required for a retrieved chunk
     */
    SkillReply(Repository repository, EmbeddingRepository embeddingRepository,
            ConnectiveAgent connectiveAgent, ExpansionAgent expansionAgent,
            int maxResults, double minScore) {
        this.repository = repository;
        this.embeddingRepository = embeddingRepository;
        this.connectiveAgent = connectiveAgent;
        this.expansionAgent = expansionAgent;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    /**
     * Streams the reply of the conversation's specialist and stores it when the
     * stream completes. The prompt is what the model sees; it is not written as a
     * student message.
     *
     * @param chat     conversation whose {@code tutorActivity} selects the agent
     * @param memoryId Redis memory id already used for this channel
     * @param prompt   student answer or internal exercise request
     * @return the assistant chunks
     * @throws IllegalStateException when the conversation has no specialist
     */
    Multi<String> answer(Chat chat, String memoryId, String prompt) {
        TutorActivity activity = chat.getTutorActivity();
        if (activity == null) {
            throw new IllegalStateException("Conversa sem habilidade");
        }

        RagQuery query = new RagQuery(prompt, maxResults, minScore);
        RagResponse ragResponse = embeddingRepository.searchChunks(query);
        String context = ragResponse.getContexts().isEmpty()
                ? DEFAULT_CONTEXT : String.join("\n\n", ragResponse.getContexts());

        Multi<String> stream = switch (activity) {
            case CONNECTIVES -> connectiveAgent.answer(memoryId, context, prompt);
            case EXPANSION -> expansionAgent.answer(memoryId, context, prompt);
        };

        StringBuilder buffer = new StringBuilder();
        return stream.invoke(buffer::append)
                .onCompletion().invoke(() -> {
                    AgentMessage agentMessage = new AgentMessage();
                    agentMessage.setMessage(buffer.toString());
                    agentMessage.setTimestamp(new Date());
                    chat.addMessage(agentMessage);
                    repository.save(chat);
                });
    }

}
