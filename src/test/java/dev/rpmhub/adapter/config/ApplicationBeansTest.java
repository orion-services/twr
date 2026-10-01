package dev.rpmhub.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.rpmhub.adapter.out.ai.ConnectiveAgent;
import dev.rpmhub.adapter.out.ai.ExpansionAgent;
import dev.rpmhub.application.ChatService;
import dev.rpmhub.application.TutorTexts;
import dev.rpmhub.domain.model.RagQuery;
import dev.rpmhub.domain.model.RagResponse;
import dev.rpmhub.domain.port.in.ChatUseCase;
import dev.rpmhub.domain.port.out.EmbeddingRepository;
import dev.rpmhub.domain.port.out.Repository;
import io.smallrye.mutiny.Multi;

/**
 * Unit tests for {@link ApplicationBeans}.
 *
 * <p>Verifies that the CDI wiring class produces a working {@link ChatUseCase}
 * backed by a plain {@link ChatService}, without booting a CDI container.
 *
 * @author Rodrigo Prestes Machado
 */
@ExtendWith(MockitoExtension.class)
class ApplicationBeansTest {

    /**
     * Driven port mocked to isolate the wiring under test.
     */
    @Mock
    private Repository chatRepository;

    /**
     * Driven port mocked to isolate the wiring under test.
     */
    @Mock
    private EmbeddingRepository embeddingRepository;

    /**
     * Connectives specialist mocked to isolate the wiring under test.
     */
    @Mock
    private ConnectiveAgent connectiveAgent;

    /**
     * Expansion specialist mocked to isolate the wiring under test.
     */
    @Mock
    private ExpansionAgent expansionAgent;

    /**
     * Wiring class under test.
     */
    private ApplicationBeans applicationBeans;

    /**
     * Wires the beans class with mocked ports before each test.
     */
    @BeforeEach
    void setUp() {
        applicationBeans = new ApplicationBeans();
        applicationBeans.chatRepository = chatRepository;
        applicationBeans.embeddingRepository = embeddingRepository;
        applicationBeans.connectiveAgent = connectiveAgent;
        applicationBeans.expansionAgent = expansionAgent;
    }

    /**
     * Ensures the produced use case is a plain, framework-free {@link ChatService}
     * wired with the injected ports.
     */
    @Test
    void chatUseCase_producesChatServiceWiredWithInjectedPorts() {
        when(chatRepository.findLastByPhone("5511999999999")).thenReturn(Optional.empty());
        when(embeddingRepository.searchChunks(org.mockito.ArgumentMatchers.any(RagQuery.class)))
                .thenReturn(new RagResponse(List.of()));
        when(connectiveAgent.answer("5511999999999", "", TutorTexts.FIRST_EXERCISE))
                .thenReturn(Multi.createFrom().items("resposta"));

        ChatUseCase chatUseCase = applicationBeans.chatUseCase();

        assertInstanceOf(ChatService.class, chatUseCase);
        List<String> chunks = chatUseCase.chat("5511999999999", "\\conectivos")
                .collect().asList().await().indefinitely();
        assertEquals(List.of("resposta"), chunks);
    }

}
