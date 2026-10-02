package dev.rpmhub.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import dev.rpmhub.domain.model.User;
import dev.rpmhub.domain.port.out.EmbeddingRepository;
import dev.rpmhub.domain.port.out.Repository;
import io.smallrye.mutiny.Multi;

/**
 * Unit tests for {@link ConversationService}.
 *
 * @author Rodrigo Prestes Machado
 */
class ConversationServiceTest {

    private static final String OWNER = "orion-hash";

    private FakeChatRepository repository;

    private RecordingConnective connectiveAgent;

    private RecordingExpansion expansionAgent;

    private ConversationService service;

    @BeforeEach
    void setUp() {
        repository = new FakeChatRepository();
        connectiveAgent = new RecordingConnective();
        expansionAgent = new RecordingExpansion();
        service = new ConversationService(repository, new FakeEmbeddingRepository(),
                connectiveAgent, expansionAgent, 3, 0.6);
    }

    /**
     * Creating a conversation stores the chosen specialist and returns it on a later read.
     */
    @Test
    void createConversation_persistsActivity_andReturnsItOnRead() {
        Chat created = service.createConversation(user(), "Conectivos", TutorActivity.CONNECTIVES);

        Chat loaded = service.getConversation(created.getId()).orElseThrow();
        assertEquals(TutorActivity.CONNECTIVES, loaded.getTutorActivity());
        assertEquals("Conectivos", loaded.getTitle());
    }

    /**
     * A web turn calls only the conversation's specialist, with the conversation id as memory id.
     * A command-shaped message is a normal student answer.
     */
    @Test
    void chat_callsOnlyTheStoredAgent_withConversationIdAsMemory() {
        Chat created = service.createConversation(user(), "Conectivos", TutorActivity.CONNECTIVES);

        List<String> chunks = service.chat(user(), created.getId(), "\\expansion")
                .collect().asList().await().indefinitely();

        assertEquals(List.of("resposta"), chunks);
        Chat loaded = service.getConversation(created.getId()).orElseThrow();
        assertEquals(TutorActivity.CONNECTIVES, loaded.getTutorActivity());
        assertEquals(List.of(created.getId()), connectiveAgent.memoryIds);
        assertEquals(List.of("\\expansion"), connectiveAgent.prompts);
        assertTrue(expansionAgent.prompts.isEmpty());
    }

    /**
     * An expansion conversation never calls the connectives specialist.
     */
    @Test
    void chat_callsExpansionAgent_whenConversationIsExpansion() {
        Chat created = service.createConversation(user(), "Expansão", TutorActivity.EXPANSION);

        service.chat(user(), created.getId(), "na biblioteca").collect().asList().await().indefinitely();

        assertEquals(List.of(created.getId()), expansionAgent.memoryIds);
        assertEquals(List.of("na biblioteca"), expansionAgent.prompts);
        assertTrue(connectiveAgent.prompts.isEmpty());
    }

    /**
     * The first exercise is requested internally and is not stored as a student message.
     */
    @Test
    void startExercise_asksForTheFirstExercise_withoutAStudentMessage() {
        Chat created = service.createConversation(user(), "Conectivos", TutorActivity.CONNECTIVES);

        List<String> chunks = service.startExercise(user(), created.getId())
                .collect().asList().await().indefinitely();

        assertEquals(List.of("resposta"), chunks);
        Chat loaded = service.getConversation(created.getId()).orElseThrow();
        assertTrue(loaded.getUserMessages().isEmpty());
        assertEquals(1, loaded.getAgentMessages().size());
        assertEquals(List.of(TutorTexts.FIRST_EXERCISE), connectiveAgent.prompts);
        assertEquals(List.of(created.getId()), connectiveAgent.memoryIds);
        assertTrue(expansionAgent.prompts.isEmpty());
    }

    /**
     * A conversation that already has messages does not ask for another opening exercise.
     */
    @Test
    void startExercise_doesNothing_whenConversationAlreadyHasMessages() {
        Chat created = service.createConversation(user(), "Conectivos", TutorActivity.CONNECTIVES);
        service.chat(user(), created.getId(), "oi").collect().asList().await().indefinitely();
        connectiveAgent.prompts.clear();

        List<String> chunks = service.startExercise(user(), created.getId())
                .collect().asList().await().indefinitely();

        assertTrue(chunks.isEmpty());
        assertTrue(connectiveAgent.prompts.isEmpty());
    }

    /**
     * An older conversation without a specialist can receive one, once.
     */
    @Test
    void assignActivity_storesTheChoice_andRefusesASecondChoice() {
        Chat legacy = Chat.start(user());
        legacy.setTitle("New Conversation");
        repository.save(legacy);

        Chat assigned = service.assignActivity(legacy.getId(), OWNER, TutorActivity.EXPANSION);

        assertEquals(TutorActivity.EXPANSION, assigned.getTutorActivity());
        assertEquals(TutorActivity.EXPANSION,
                service.getConversation(legacy.getId()).orElseThrow().getTutorActivity());
        assertThrows(IllegalStateException.class,
                () -> service.assignActivity(legacy.getId(), OWNER, TutorActivity.CONNECTIVES));
    }

    private static User user() {
        User user = new User();
        user.setOrionUserHash(OWNER);
        user.setEmail("aluno@example.com");
        return user;
    }

    /**
     * In-memory repository used as a test double.
     */
    private static final class FakeChatRepository implements Repository {

        private final ConcurrentMap<String, Chat> chatsById = new ConcurrentHashMap<>();

        @Override
        public Optional<Chat> findLastByPhone(String phoneNumber) {
            return Optional.empty();
        }

        @Override
        public void save(Chat chat) {
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
                    .toList();
        }

        @Override
        public void deleteConversation(String id) {
            chatsById.remove(id);
        }
    }

    /**
     * Fake embedding repository that returns no context.
     */
    private static final class FakeEmbeddingRepository implements EmbeddingRepository {

        @Override
        public RagResponse searchChunks(RagQuery query) {
            return new RagResponse(List.of());
        }
    }

    /**
     * Records calls made to the connectives specialist.
     */
    private static final class RecordingConnective implements ConnectiveAgent {

        private final List<String> memoryIds = new ArrayList<>();

        private final List<String> prompts = new ArrayList<>();

        @Override
        public Multi<String> answer(String memoryId, String context, String prompt) {
            memoryIds.add(memoryId);
            prompts.add(prompt);
            return Multi.createFrom().item("resposta");
        }
    }

    /**
     * Records calls made to the expansion specialist.
     */
    private static final class RecordingExpansion implements ExpansionAgent {

        private final List<String> memoryIds = new ArrayList<>();

        private final List<String> prompts = new ArrayList<>();

        @Override
        public Multi<String> answer(String memoryId, String context, String prompt) {
            memoryIds.add(memoryId);
            prompts.add(prompt);
            return Multi.createFrom().item("resposta");
        }
    }

}
