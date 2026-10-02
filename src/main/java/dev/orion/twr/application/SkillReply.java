/*
 * Copyright 2026 Rodrigo Prestes Machado
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.orion.twr.application;

import java.util.Date;

import dev.orion.twr.adapter.out.ai.ConnectiveAgent;
import dev.orion.twr.adapter.out.ai.ExpansionAgent;
import dev.orion.twr.domain.model.AgentMessage;
import dev.orion.twr.domain.model.Chat;
import dev.orion.twr.domain.model.RagQuery;
import dev.orion.twr.domain.model.RagResponse;
import dev.orion.twr.domain.model.TutorActivity;
import dev.orion.twr.domain.port.out.EmbeddingRepository;
import dev.orion.twr.domain.port.out.Repository;
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
