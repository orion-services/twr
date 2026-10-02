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
import dev.orion.twr.domain.model.TutorActivity;
import dev.orion.twr.domain.model.TutorCommand;
import dev.orion.twr.domain.model.User;
import dev.orion.twr.domain.model.UserMessage;
import dev.orion.twr.domain.port.in.ChatUseCase;
import dev.orion.twr.domain.port.out.EmbeddingRepository;
import dev.orion.twr.domain.port.out.Repository;
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
 * {@code dev.orion.twr.adapter.config.ApplicationBeans}.
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
