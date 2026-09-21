package dev.rpmhub.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import dev.rpmhub.domain.port.in.ChatUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Quarkus tests that verify the REST adapter wiring.
 *
 * @author Rodrigo Prestes Machado
 */
@QuarkusTest
class AgentResourceTest {

    /**
     * Driving port injected by CDI to validate hexagonal wiring.
     */
    @Inject
    ChatUseCase chatUseCase;

    /**
     * Ensures the application boots and the chat use case is available.
     */
    @Test
    void chatUseCaseIsInjected() {
        assertNotNull(chatUseCase);
    }

}
