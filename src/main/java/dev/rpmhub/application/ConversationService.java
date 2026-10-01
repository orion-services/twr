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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import dev.rpmhub.adapter.out.ai.ConnectiveAgent;
import dev.rpmhub.adapter.out.ai.ExpansionAgent;
import dev.rpmhub.domain.model.Chat;
import dev.rpmhub.domain.model.TutorActivity;
import dev.rpmhub.domain.model.User;
import dev.rpmhub.domain.model.UserMessage;
import dev.rpmhub.domain.port.in.ConversationUseCase;
import dev.rpmhub.domain.port.out.EmbeddingRepository;
import dev.rpmhub.domain.port.out.Repository;
import io.smallrye.mutiny.Multi;

/**
 * Application service that orchestrates the authenticated web conversation flow:
 * explicit, user-created conversations, each answered by a single specialist
 * chosen at creation. Memory is isolated by conversation id.
 *
 * <p>Framework-agnostic (plain Java), wired by
 * {@code dev.rpmhub.adapter.config.ApplicationBeans}.
 *
 * @author Rodrigo Prestes Machado
 */
public class ConversationService implements ConversationUseCase {

    /** Repository used to load and store conversations. */
    private final Repository repository;

    /** Calls the specialist stored on the conversation. */
    private final SkillReply skillReply;

    /**
     * Creates the conversation service with its driven ports.
     *
     * @param repository          port used to persist conversations
     * @param embeddingRepository port for vector-similarity search
     * @param connectiveAgent     specialist for connectives
     * @param expansionAgent      specialist for sentence expansion
     * @param maxResults          number of context chunks retrieved per message
     * @param minScore            minimum similarity score required for a retrieved chunk
     */
    public ConversationService(Repository repository, EmbeddingRepository embeddingRepository,
            ConnectiveAgent connectiveAgent, ExpansionAgent expansionAgent,
            int maxResults, double minScore) {
        this.repository = repository;
        this.skillReply = new SkillReply(repository, embeddingRepository, connectiveAgent, expansionAgent,
                maxResults, minScore);
    }

    @Override
    public Chat createConversation(User user, String title, TutorActivity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("A conversa precisa de uma habilidade");
        }
        Chat chat = Chat.start(user);
        chat.setTitle(title);
        chat.setTutorActivity(activity);
        repository.save(chat);
        return chat;
    }

    @Override
    public List<Chat> listConversations(String orionUserHash) {
        return repository.findAllByOrionUserHash(orionUserHash);
    }

    @Override
    public Optional<Chat> getConversation(String conversationId) {
        return repository.findConversationById(conversationId);
    }

    @Override
    public Chat renameConversation(String conversationId, String orionUserHash, String title) {
        Chat chat = ownedConversation(conversationId, orionUserHash);
        chat.setTitle(title);
        repository.save(chat);
        return chat;
    }

    @Override
    public Chat assignActivity(String conversationId, String orionUserHash, TutorActivity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("A conversa precisa de uma habilidade");
        }
        Chat chat = ownedConversation(conversationId, orionUserHash);
        if (chat.getTutorActivity() != null) {
            throw new IllegalStateException("A habilidade desta conversa já foi escolhida");
        }
        chat.setTutorActivity(activity);
        repository.save(chat);
        return chat;
    }

    @Override
    public void deleteConversation(String conversationId, String orionUserHash) {
        ownedConversation(conversationId, orionUserHash);
        repository.deleteConversation(conversationId);
    }

    @Override
    public Multi<String> startExercise(User user, String conversationId) {
        Chat chat = ownedConversation(conversationId, user.getOrionUserHash());
        if (chat.getTutorActivity() == null) {
            throw new IllegalStateException("Conversa sem habilidade");
        }
        if (!chat.getMessages().isEmpty()) {
            return Multi.createFrom().empty();
        }
        return skillReply.answer(chat, conversationId, TutorTexts.FIRST_EXERCISE);
    }

    @Override
    public Multi<String> chat(User user, String conversationId, String prompt) {
        Chat chat = ownedConversation(conversationId, user.getOrionUserHash());
        if (chat.getTutorActivity() == null) {
            throw new IllegalStateException("Conversa sem habilidade");
        }

        UserMessage userMessage = new UserMessage();
        userMessage.setUser(user);
        userMessage.setMessage(prompt);
        userMessage.setTimestamp(new Date());
        chat.addMessage(userMessage);
        repository.save(chat);

        return skillReply.answer(chat, conversationId, prompt);
    }

    /**
     * Loads the conversation and validates that it belongs to the given owner.
     *
     * @param conversationId the conversation id
     * @param orionUserHash  the Orion Users hash of the caller
     * @return the owned conversation
     * @throws NoSuchElementException if the conversation does not exist
     * @throws SecurityException      if the conversation belongs to a different owner
     */
    private Chat ownedConversation(String conversationId, String orionUserHash) {
        Chat chat = repository.findConversationById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversa não encontrada: " + conversationId));
        String owner = chat.getUser() != null ? chat.getUser().getOrionUserHash() : null;
        if (owner == null || !owner.equals(orionUserHash)) {
            throw new SecurityException("Acesso negado à conversa " + conversationId);
        }
        return chat;
    }

}
