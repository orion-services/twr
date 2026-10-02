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
import dev.rpmhub.domain.model.TutorActivity;
import dev.rpmhub.domain.model.TutorCommand;
import dev.rpmhub.domain.model.User;
import dev.rpmhub.domain.model.UserMessage;
import dev.rpmhub.domain.port.in.ChatUseCase;
import dev.rpmhub.domain.port.out.EmbeddingRepository;
import dev.rpmhub.domain.port.out.Repository;
import io.smallrye.mutiny.Multi;

/**
 * Application service that orchestrates the WhatsApp chat flow.
 *
 * <p>A new conversation starts when the student has been idle longer than the
 * inactivity threshold. That conversation has no specialist until the student
 * sends {@code \connectives} or {@code \expansion}. Redis memory stays keyed by
 * the phone number.
 *
 * <p>Framework-agnostic (plain Java), wired by
 * {@code dev.rpmhub.adapter.config.ApplicationBeans}.
 *
 * @author Rodrigo Prestes Machado
 */
public class ChatService implements ChatUseCase {

    /** Repository used to load and store the last chat per phone number. */
    private final Repository chatRepository;

    /** Calls the specialist stored on the conversation. */
    private final SkillReply skillReply;

    /** Maximum idle time, in milliseconds, before a new chat session starts. */
    private final long inactivityThresholdMs;

    /**
     * Creates the chat service with its driven ports.
     *
     * @param chatRepository        port used to persist chats
     * @param embeddingRepository   port for vector-similarity search
     * @param connectiveAgent       specialist for connectives
     * @param expansionAgent        specialist for sentence expansion
     * @param maxResults            number of context chunks retrieved per message
     * @param minScore              minimum similarity score required for a retrieved chunk
     * @param inactivityThresholdMs maximum idle time, in milliseconds, before a new chat session starts
     */
    public ChatService(Repository chatRepository, EmbeddingRepository embeddingRepository,
            ConnectiveAgent connectiveAgent, ExpansionAgent expansionAgent,
            int maxResults, double minScore, long inactivityThresholdMs) {
        this.chatRepository = chatRepository;
        this.skillReply = new SkillReply(chatRepository, embeddingRepository, connectiveAgent, expansionAgent,
                maxResults, minScore);
        this.inactivityThresholdMs = inactivityThresholdMs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multi<String> chat(String phoneNumber, String message) {
        User user = new User();
        user.setPhoneNumber(phoneNumber);

        UserMessage userMessage = new UserMessage();
        userMessage.setUser(user);
        userMessage.setMessage(message);
        userMessage.setTimestamp(new Date());

        Chat lastChat = chatRepository.findLastByPhone(phoneNumber).orElse(null);
        Chat chat = Chat.accept(lastChat, userMessage, inactivityThresholdMs);

        TutorActivity command = TutorCommand.parse(message);
        if (command != null) {
            boolean firstChoice = chat.getTutorActivity() == null;
            chat.setTutorActivity(command);
            chatRepository.save(chat);
            String prompt = firstChoice ? TutorTexts.FIRST_EXERCISE : TutorTexts.NEXT_EXERCISE;
            return skillReply.answer(chat, phoneNumber, prompt);
        }

        chatRepository.save(chat);
        if (chat.getTutorActivity() == null) {
            return fixedReply(chat, TutorTexts.WHATSAPP_CHOICE);
        }
        return skillReply.answer(chat, phoneNumber, message);
    }

    /**
     * Stores a fixed assistant message and returns it as a single chunk.
     *
     * @param chat the conversation that already contains the student message
     * @param text the assistant text
     * @return a multi that emits {@code text}
     */
    private Multi<String> fixedReply(Chat chat, String text) {
        AgentMessage agentMessage = new AgentMessage();
        agentMessage.setMessage(text);
        agentMessage.setTimestamp(new Date());
        chat.addMessage(agentMessage);
        chatRepository.save(chat);
        return Multi.createFrom().item(text);
    }

}
