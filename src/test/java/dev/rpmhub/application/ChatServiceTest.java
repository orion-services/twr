package dev.rpmhub.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.rpmhub.adapter.out.ai.ConnectiveAgent;
import dev.rpmhub.adapter.out.ai.ExpansionAgent;
import dev.rpmhub.domain.model.Chat;
import dev.rpmhub.domain.model.RagQuery;
import dev.rpmhub.domain.model.RagResponse;
import dev.rpmhub.domain.model.TutorActivity;
import dev.rpmhub.domain.port.out.EmbeddingRepository;
import dev.rpmhub.domain.port.out.Repository;
import io.smallrye.mutiny.Multi;

/**
 * Unit tests for {@link ChatService}.
 *
 * @author Rodrigo Prestes Machado
 */
class ChatServiceTest {

    /**
     * Number of milliseconds in one minute.
     */
    private static final long MINUTE_MS = 60_000L;

    /**
     * Phone number used by every test.
     */
    private static final String PHONE = "5511999999999";

    /**
     * In-memory repository used as a test double.
     */
    private FakeChatRepository chatRepository;

    /**
     * Embedding repository test double that returns an empty context by default.
     */
    private FakeEmbeddingRepository embeddingRepository;

    /**
     * Connectives specialist test double.
     */
    private RecordingConnective connectiveAgent;

    /**
     * Expansion specialist test double.
     */
    private RecordingExpansion expansionAgent;

    /**
     * Service under test.
     */
    private ChatService chatService;

    /**
     * Prepares fakes and the service before each test.
     */
    @BeforeEach
    void setUp() {
        chatRepository = new FakeChatRepository();
        embeddingRepository = new FakeEmbeddingRepository();
        connectiveAgent = new RecordingConnective();
        expansionAgent = new RecordingExpansion();
        chatService = new ChatService(chatRepository, embeddingRepository, connectiveAgent, expansionAgent,
                3, 0.6, 30 * MINUTE_MS);
    }

    /**
     * A first message that is not a command opens a conversation and asks for a choice.
     */
    @Test
    void chat_asksForChoice_whenConversationHasNoActivity() {
        List<String> chunks = chatService.chat(PHONE, "oi").collect().asList().await().indefinitely();

        assertEquals(List.of(TutorTexts.WHATSAPP_CHOICE), chunks);
        assertTrue(connectiveAgent.prompts.isEmpty());
        assertTrue(expansionAgent.prompts.isEmpty());
        Chat chat = chatRepository.findLastByPhone(PHONE).orElseThrow();
        assertNull(chat.getTutorActivity());
        assertEquals("oi", chat.getUserMessages().get(0).getMessage());
        assertEquals(TutorTexts.WHATSAPP_CHOICE, chat.getAgentMessages().get(0).getMessage());
    }

    /**
     * Ensures a follow-up within thirty minutes reuses the same chat.
     */
    @Test
    void chat_reusesChat_whenWithinInactivityThreshold() {
        chatService.chat(PHONE, "\\conectivos").collect().asList().await().indefinitely();
        Chat first = chatRepository.findLastByPhone(PHONE).orElseThrow();

        chatService.chat(PHONE, "porque estava doente").collect().asList().await().indefinitely();
        Chat second = chatRepository.findLastByPhone(PHONE).orElseThrow();

        assertSame(first, second);
        assertEquals(TutorActivity.CONNECTIVES, second.getTutorActivity());
        assertEquals(2, second.getUserMessages().size());
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE, "porque estava doente"), connectiveAgent.prompts);
        assertEquals(List.of(PHONE, PHONE), connectiveAgent.memoryIds);
        assertTrue(expansionAgent.prompts.isEmpty());
    }

    /**
     * Ensures a new chat is opened when the idle time exceeds thirty minutes,
     * without a specialist and without calling the previous agent.
     */
    @Test
    void chat_opensNewChat_whenIdleMoreThanThirtyMinutes() {
        chatService.chat(PHONE, "\\conectivos").collect().asList().await().indefinitely();
        Chat first = chatRepository.findLastByPhone(PHONE).orElseThrow();
        first.getUserMessages().get(0).setTimestamp(
                new java.util.Date(System.currentTimeMillis() - (31 * MINUTE_MS)));

        List<String> chunks = chatService.chat(PHONE, "voltei").collect().asList().await().indefinitely();
        Chat next = chatRepository.findLastByPhone(PHONE).orElseThrow();

        assertNotEquals(first.getId(), next.getId());
        assertNull(next.getTutorActivity());
        assertEquals(List.of(TutorTexts.WHATSAPP_CHOICE), chunks);
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE), connectiveAgent.prompts);
        assertTrue(expansionAgent.prompts.isEmpty());
    }

    /**
     * {@code \conectivos} stores the specialist and asks it for the first exercise.
     * The command itself is not the prompt the model sees.
     */
    @Test
    void chat_selectsConnectives_andKeepsPhoneAsMemoryId() {
        connectiveAgent.chunks = List.of("res", "pos", "ta");

        List<String> chunks = chatService.chat(PHONE, "\\conectivos").collect().asList().await().indefinitely();

        assertEquals(List.of("res", "pos", "ta"), chunks);
        Chat chat = chatRepository.findLastByPhone(PHONE).orElseThrow();
        assertEquals(TutorActivity.CONNECTIVES, chat.getTutorActivity());
        assertEquals("\\conectivos", chat.getUserMessages().get(0).getMessage());
        assertEquals("resposta", chat.getAgentMessages().get(0).getMessage());
        assertEquals(List.of(PHONE), connectiveAgent.memoryIds);
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE), connectiveAgent.prompts);
        assertTrue(expansionAgent.prompts.isEmpty());
    }

    /**
     * {@code \expansao} selects the expansion specialist. An accented command counts too.
     */
    @Test
    void chat_selectsExpansion_whenCommandHasAnAccent() {
        chatService.chat(PHONE, "\\expansão").collect().asList().await().indefinitely();

        Chat chat = chatRepository.findLastByPhone(PHONE).orElseThrow();
        assertEquals(TutorActivity.EXPANSION, chat.getTutorActivity());
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE), expansionAgent.prompts);
        assertEquals(List.of(PHONE), expansionAgent.memoryIds);
        assertTrue(connectiveAgent.prompts.isEmpty());
    }

    /**
     * A later command switches the specialist and does not send the command as the answer.
     * Redis stays on the phone number.
     */
    @Test
    void chat_switchesAgent_whenCommandChangesActivity() {
        chatService.chat(PHONE, "\\conectivos").collect().asList().await().indefinitely();
        Chat first = chatRepository.findLastByPhone(PHONE).orElseThrow();

        chatService.chat(PHONE, "\\expansao").collect().asList().await().indefinitely();
        Chat second = chatRepository.findLastByPhone(PHONE).orElseThrow();

        assertSame(first, second);
        assertEquals(TutorActivity.EXPANSION, second.getTutorActivity());
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE), connectiveAgent.prompts);
        assertEquals(List.of(TutorTexts.NEXT_EXERCISE), expansionAgent.prompts);
        assertEquals(List.of(PHONE), expansionAgent.memoryIds);
    }

    /**
     * A normal answer does not change the specialist.
     */
    @Test
    void chat_keepsActivity_whenMessageIsNotACommand() {
        chatService.chat(PHONE, "\\conectivos").collect().asList().await().indefinitely();
        chatService.chat(PHONE, "quero \\expansao no meio").collect().asList().await().indefinitely();

        Chat chat = chatRepository.findLastByPhone(PHONE).orElseThrow();
        assertEquals(TutorActivity.CONNECTIVES, chat.getTutorActivity());
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE, "quero \\expansao no meio"), connectiveAgent.prompts);
        assertTrue(expansionAgent.prompts.isEmpty());
    }

    /**
     * Ensures the retrieved RAG context is forwarded to the selected specialist.
     */
    @Test
    void chat_forwardsRetrievedContext_toSelectedAgent() {
        embeddingRepository.contexts = List.of("trecho relevante");

        chatService.chat(PHONE, "\\conectivos").collect().asList().await().indefinitely();

        assertEquals(List.of("trecho relevante"), connectiveAgent.contexts);
        assertTrue(expansionAgent.contexts.isEmpty());
    }

    /**
     * Fake repository that stores the last chat per phone number.
     */
    private static final class FakeChatRepository implements Repository {

        /**
         * Last chat indexed by phone number.
         */
        private final ConcurrentMap<String, Chat> chatsByPhone = new ConcurrentHashMap<>();

        /**
         * All chats indexed by id, used by the conversation-based (web) methods.
         */
        private final ConcurrentMap<String, Chat> chatsById = new ConcurrentHashMap<>();

        @Override
        public Optional<Chat> findLastByPhone(String phoneNumber) {
            return Optional.ofNullable(chatsByPhone.get(phoneNumber));
        }

        @Override
        public void save(Chat chat) {
            if (chat.getUser().getPhoneNumber() != null) {
                chatsByPhone.put(chat.getUser().getPhoneNumber(), chat);
            }
            chatsById.put(chat.getId(), chat);
        }

        @Override
        public Optional<Chat> findConversationById(String id) {
            return Optional.ofNullable(chatsById.get(id));
        }

        @Override
        public List<Chat> findAllByOrionUserHash(String orionUserHash) {
            return chatsById.values().stream()
                    .filter(c -> orionUserHash.equals(c.getUser().getOrionUserHash()))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public void deleteConversation(String id) {
            chatsById.remove(id);
        }
    }

    /**
     * Fake embedding repository that returns a configurable list of contexts.
     */
    private static final class FakeEmbeddingRepository implements EmbeddingRepository {

        /**
         * Contexts returned by the next call to {@link #searchChunks(RagQuery)}.
         */
        private List<String> contexts = List.of();

        @Override
        public RagResponse searchChunks(RagQuery query) {
            return new RagResponse(contexts);
        }
    }

    /**
     * Records calls made to the connectives specialist.
     */
    private static final class RecordingConnective implements ConnectiveAgent {

        private final List<String> memoryIds = new ArrayList<>();

        private final List<String> prompts = new ArrayList<>();

        private final List<String> contexts = new ArrayList<>();

        private List<String> chunks = List.of("resposta");

        @Override
        public Multi<String> answer(String memoryId, String context, String prompt) {
            memoryIds.add(memoryId);
            contexts.add(context);
            prompts.add(prompt);
            return Multi.createFrom().iterable(chunks);
        }
    }

    /**
     * Records calls made to the expansion specialist.
     */
    private static final class RecordingExpansion implements ExpansionAgent {

        private final List<String> memoryIds = new ArrayList<>();

        private final List<String> prompts = new ArrayList<>();

        private final List<String> contexts = new ArrayList<>();

        private List<String> chunks = List.of("resposta");

        @Override
        public Multi<String> answer(String memoryId, String context, String prompt) {
            memoryIds.add(memoryId);
            contexts.add(context);
            prompts.add(prompt);
            return Multi.createFrom().iterable(chunks);
        }
    }

}
