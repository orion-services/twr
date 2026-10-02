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
package dev.orion.twr.adapter.out.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Base JPA entity that persists a message exchanged inside a chat.
 *
 * <p>Concrete subtypes ({@link UserMessageEntity} and
 * {@link AgentMessageEntity}) share the same table, distinguished by a
 * discriminator column, so that all messages of a chat can be loaded and
 * ordered together.</p>
 *
 * @author Rodrigo Prestes Machado
 */
@Entity
@Table(name = "message")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "message_type", length = 16)
public abstract class MessageEntity {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent chat that owns this message.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    /**
     * Text content of the message.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Instant when the message was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Order of the message inside the chat.
     */
    @Column(name = "sequence", nullable = false)
    private int sequence;

    /**
     * Returns the surrogate id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the surrogate id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the parent chat.
     *
     * @return the chat entity
     */
    public ChatEntity getChat() {
        return chat;
    }

    /**
     * Sets the parent chat.
     *
     * @param chat the chat entity to set
     */
    public void setChat(ChatEntity chat) {
        this.chat = chat;
    }

    /**
     * Returns the message text.
     *
     * @return the message content
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message text.
     *
     * @param message the message content to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the message timestamp.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the message timestamp.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the message order inside the chat.
     *
     * @return the sequence index
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Sets the message order inside the chat.
     *
     * @param sequence the sequence index to set
     */
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

}
