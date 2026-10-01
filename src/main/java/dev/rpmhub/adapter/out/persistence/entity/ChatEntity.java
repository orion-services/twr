/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.rpmhub.domain.model.TutorActivity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * JPA entity that persists a chat session.
 *
 * @author Rodrigo Prestes Machado
 */
@Entity
@Table(name = "chat")
public class ChatEntity {

    /**
     * Unique identifier of this chat.
     */
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /**
     * Phone number that identifies the chat owner (WhatsApp channel). Null for chats
     * created through the authenticated web channel.
     */
    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    /**
     * Orion Users hash that identifies the chat owner (web channel). Null for chats
     * created through the WhatsApp channel.
     */
    @Column(name = "orion_user_hash", length = 128)
    private String orionUserHash;

    /**
     * Email of the chat owner, as reported by the Orion Users JWT (web channel only).
     */
    @Column(name = "user_email", length = 255)
    private String userEmail;

    /**
     * Human-readable title of the conversation (web channel only).
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * Specialist that answers this conversation. Null until the student chooses one.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_activity", length = 32)
    private TutorActivity tutorActivity;

    /**
     * Instant when this chat was started.
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * Messages that belong to this chat, in chronological order.
     */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sequence Asc")
    private List<MessageEntity> messages = new ArrayList<>();

    /**
     * Returns the chat identifier.
     *
     * @return the chat id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the chat identifier.
     *
     * @param id the chat id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the owner phone number.
     *
     * @return the phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the owner phone number.
     *
     * @param phoneNumber the phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the owner's Orion Users hash.
     *
     * @return the Orion Users hash
     */
    public String getOrionUserHash() {
        return orionUserHash;
    }

    /**
     * Sets the owner's Orion Users hash.
     *
     * @param orionUserHash the Orion Users hash to set
     */
    public void setOrionUserHash(String orionUserHash) {
        this.orionUserHash = orionUserHash;
    }

    /**
     * Returns the owner's email.
     *
     * @return the email
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * Sets the owner's email.
     *
     * @param userEmail the email to set
     */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    /**
     * Returns the conversation title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the conversation title.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the specialist assigned to this conversation.
     *
     * @return the activity, or {@code null} when not chosen
     */
    public TutorActivity getTutorActivity() {
        return tutorActivity;
    }

    /**
     * Sets the specialist assigned to this conversation.
     *
     * @param tutorActivity the activity to set
     */
    public void setTutorActivity(TutorActivity tutorActivity) {
        this.tutorActivity = tutorActivity;
    }

    /**
     * Returns the chat start timestamp.
     *
     * @return the start timestamp
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Sets the chat start timestamp.
     *
     * @param startedAt the start timestamp to set
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Returns the persisted messages.
     *
     * @return the message entities
     */
    public List<MessageEntity> getMessages() {
        return messages;
    }

    /**
     * Sets the persisted messages.
     *
     * @param messages the message entities to set
     */
    public void setMessages(List<MessageEntity> messages) {
        this.messages = messages;
    }

}
