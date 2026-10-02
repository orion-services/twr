package dev.orion.twr.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code getType()} role discriminator exposed by
 * {@link Message} subtypes.
 *
 * <p>This discriminator is what lets the frontend tell apart user and
 * assistant messages when reloading persisted conversation history via
 * {@code GET /twr/memory} (see
 * {@code dev.orion.twr.adapter.in.rest.dto.MemoryResponse}). Without it, every
 * message serializes identically and the whole reloaded dialogue collapses
 * into a single (assistant) role on the frontend.</p>
 *
 * @author Rodrigo Prestes Machado
 */
class MessageTest {

    /**
     * Ensures {@link UserMessage} reports the {@code "USER"} role.
     */
    @Test
    void getType_returnsUser_forUserMessage() {
        assertEquals("USER", new UserMessage().getType());
    }

    /**
     * Ensures {@link AgentMessage} reports the {@code "AGENT"} role.
     */
    @Test
    void getType_returnsAgent_forAgentMessage() {
        assertEquals("AGENT", new AgentMessage().getType());
    }

}
