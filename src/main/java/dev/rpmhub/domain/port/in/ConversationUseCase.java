/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.domain.port.in;

import java.util.List;
import java.util.Optional;

import dev.rpmhub.domain.model.Chat;
import dev.rpmhub.domain.model.TutorActivity;
import dev.rpmhub.domain.model.User;
import io.smallrye.mutiny.Multi;

/**
 * Driving port for the authenticated (web/Orion Users) conversation flow: multiple
 * named conversations per user, each with its own isolated memory, as opposed to the
 * single inactivity-based chat used by the WhatsApp channel (see {@link ChatUseCase}).
 *
 * @author Rodrigo Prestes Machado
 */
public interface ConversationUseCase {

    /**
     * Creates a new, empty conversation for the given user.
     *
     * @param user     the authenticated user (must have an Orion Users hash)
     * @param title    human-readable title for the conversation
     * @param activity specialist that will answer this conversation
     * @return the created conversation
     */
    Chat createConversation(User user, String title, TutorActivity activity);

    /**
     * Lists all conversations owned by the given Orion Users hash, most recent first.
     *
     * @param orionUserHash the Orion Users hash that identifies the owner
     * @return the owner's conversations
     */
    List<Chat> listConversations(String orionUserHash);

    /**
     * Retrieves a single conversation by id.
     *
     * @param conversationId the conversation id
     * @return the conversation, or empty if not found
     */
    Optional<Chat> getConversation(String conversationId);

    /**
     * Renames a conversation, if it belongs to the given owner.
     *
     * @param conversationId the conversation id
     * @param orionUserHash  the Orion Users hash of the caller, for ownership validation
     * @param title          the new title
     * @return the updated conversation
     */
    Chat renameConversation(String conversationId, String orionUserHash, String title);

    /**
     * Stores the specialist of a conversation that does not have one yet.
     *
     * @param conversationId the conversation id
     * @param orionUserHash  the Orion Users hash of the caller, for ownership validation
     * @param activity       specialist that will answer this conversation
     * @return the updated conversation
     * @throws IllegalStateException when the conversation already has a specialist
     */
    Chat assignActivity(String conversationId, String orionUserHash, TutorActivity activity);

    /**
     * Deletes a conversation, if it belongs to the given owner.
     *
     * @param conversationId the conversation id
     * @param orionUserHash  the Orion Users hash of the caller, for ownership validation
     */
    void deleteConversation(String conversationId, String orionUserHash);

    /**
     * Asks the conversation's specialist for the first exercise when the conversation
     * has no messages yet. The internal request is not stored as a student message.
     *
     * @param user           the authenticated user
     * @param conversationId the target conversation, which must belong to the user
     * @return a multi that emits the first exercise, or an empty multi when the
     *         conversation already has messages
     */
    Multi<String> startExercise(User user, String conversationId);

    /**
     * Accepts a prompt within an existing conversation and streams the assistant's
     * RAG-grounded reply, isolating conversational memory per conversation id.
     *
     * @param user           the authenticated user
     * @param conversationId the target conversation, which must belong to the user
     * @param prompt         the student's message
     * @return a multi that emits the assistant response as plain text chunks
     */
    Multi<String> chat(User user, String conversationId, String prompt);

}
