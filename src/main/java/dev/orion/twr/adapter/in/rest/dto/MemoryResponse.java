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
package dev.orion.twr.adapter.in.rest.dto;

import java.util.List;

import dev.orion.twr.domain.model.Chat;
import dev.orion.twr.domain.model.Message;
import dev.orion.twr.domain.model.TutorActivity;

/**
 * Response body for {@code GET /twr/memory}: the persisted message history of a
 * conversation, mirroring the shape the frontend expects from the RAG chatbot memory
 * endpoint.
 *
 * @author Rodrigo Prestes Machado
 */
public class MemoryResponse {

    /** Orion Users hash of the conversation owner. */
    public String userId;

    /** Identifier of the conversation this memory belongs to. */
    public String conversationId;

    /**
     * Specialist that answers this conversation, or {@code null} when the student
     * has not chosen yet.
     */
    public TutorActivity tutorActivity;

    /** Messages in chronological order. */
    public List<Message> messages;

    /**
     * Builds a memory response from the given conversation.
     *
     * @param chat the conversation to expose
     * @return the memory response
     */
    public static MemoryResponse fromChat(Chat chat) {
        MemoryResponse response = new MemoryResponse();
        response.userId = chat.getUser() != null ? chat.getUser().getOrionUserHash() : null;
        response.conversationId = chat.getId();
        response.tutorActivity = chat.getTutorActivity();
        response.messages = chat.getMessages();
        return response;
    }

}
