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
package dev.orion.twr.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.orion.twr.adapter.out.persistence.entity.AgentMessageEntity;
import dev.orion.twr.adapter.out.persistence.entity.ChatEntity;
import dev.orion.twr.adapter.out.persistence.entity.MessageEntity;
import dev.orion.twr.adapter.out.persistence.entity.UserMessageEntity;
import dev.orion.twr.domain.model.AgentMessage;
import dev.orion.twr.domain.model.Chat;
import dev.orion.twr.domain.model.Message;
import dev.orion.twr.domain.model.User;
import dev.orion.twr.domain.model.UserMessage;
import dev.orion.twr.domain.port.out.Repository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * PostgreSQL adapter that persists chat sessions with Hibernate Panache.
 *
 * @author Rodrigo Prestes Machado
 */
@ApplicationScoped
public class ChatRepository implements Repository, PanacheRepositoryBase<ChatEntity, String> {

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Optional<Chat> findLastByPhone(String phoneNumber) {
        return find("phoneNumber = ?1 order by startedAt desc", phoneNumber)
                .firstResultOptional()
                .map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void save(Chat chat) {
        if (chat == null || chat.getUser() == null
                || (chat.getUser().getPhoneNumber() == null && chat.getUser().getOrionUserHash() == null)) {
            throw new IllegalArgumentException(
                    "Chat must have a user with a phone number or an Orion Users hash");
        }

        ChatEntity entity = findById(chat.getId());
        boolean isNew = entity == null;
        if (isNew) {
            entity = new ChatEntity();
            entity.setId(chat.getId());
        }

        entity.setPhoneNumber(chat.getUser().getPhoneNumber());
        entity.setOrionUserHash(chat.getUser().getOrionUserHash());
        entity.setUserEmail(chat.getUser().getEmail());
        entity.setTitle(chat.getTitle());
        entity.setTutorActivity(chat.getTutorActivity());
        entity.setStartedAt(toInstant(chat.getStartedAt()));
        entity.getMessages().clear();

        if (isNew) {
            persist(entity);
        }

        List<Message> messages = chat.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            MessageEntity messageEntity = toEntity(message);
            messageEntity.setChat(entity);
            messageEntity.setMessage(message.getMessage());
            messageEntity.setCreatedAt(toInstant(message.getTimestamp()));
            messageEntity.setSequence(i);
            entity.getMessages().add(messageEntity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Optional<Chat> findConversationById(String id) {
        return Optional.ofNullable(findById(id)).map(this::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Chat> findAllByOrionUserHash(String orionUserHash) {
        return find("orionUserHash = ?1 order by startedAt desc", orionUserHash)
                .<ChatEntity>list()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteConversation(String id) {
        deleteById(id);
    }

    /**
     * Creates the persistence entity that matches the domain message type.
     *
     * @param message the domain message to convert
     * @return a new, still unpopulated-with-common-fields message entity
     */
    private MessageEntity toEntity(Message message) {
        if (message instanceof UserMessage) {
            return new UserMessageEntity();
        }
        return new AgentMessageEntity();
    }

    /**
     * Maps a persistence entity to the domain chat aggregate.
     *
     * @param entity the chat entity
     * @return the domain chat
     */
    private Chat toDomain(ChatEntity entity) {
        User user = new User();
        user.setPhoneNumber(entity.getPhoneNumber());
        user.setOrionUserHash(entity.getOrionUserHash());
        user.setEmail(entity.getUserEmail());

        Chat chat = new Chat();
        chat.setId(entity.getId());
        chat.setUser(user);
        chat.setTitle(entity.getTitle());
        chat.setTutorActivity(entity.getTutorActivity());
        chat.setStartedAt(toDate(entity.getStartedAt()));

        List<Message> messages = new ArrayList<>();
        for (MessageEntity messageEntity : entity.getMessages()) {
            Message message = toDomainMessage(messageEntity, user);
            message.setMessage(messageEntity.getMessage());
            message.setTimestamp(toDate(messageEntity.getCreatedAt()));
            message.setChat(chat);
            messages.add(message);
        }
        chat.setMessages(messages);
        return chat;
    }

    /**
     * Creates the domain message that matches the persistence entity type.
     *
     * @param messageEntity the persisted message entity
     * @param user the chat owner, used for user messages
     * @return a new, still unpopulated-with-common-fields domain message
     */
    private Message toDomainMessage(MessageEntity messageEntity, User user) {
        if (messageEntity instanceof UserMessageEntity) {
            UserMessage userMessage = new UserMessage();
            userMessage.setUser(user);
            return userMessage;
        }
        return new AgentMessage();
    }

    /**
     * Converts a domain {@link Date} to an {@link Instant}.
     *
     * @param date the date to convert
     * @return the instant, or {@code null} when the date is null
     */
    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    /**
     * Converts an {@link Instant} to a domain {@link Date}.
     *
     * @param instant the instant to convert
     * @return the date, or {@code null} when the instant is null
     */
    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

}
