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
package dev.orion.twr.adapter.in.rest;

import java.util.List;
import java.util.NoSuchElementException;

import dev.orion.twr.adapter.in.rest.dto.ActivityRequest;
import dev.orion.twr.adapter.in.rest.dto.ChatbotRequest;
import dev.orion.twr.adapter.in.rest.dto.ConversationRequest;
import dev.orion.twr.adapter.in.rest.dto.MemoryResponse;
import dev.orion.twr.domain.model.Chat;
import dev.orion.twr.domain.model.TutorActivity;
import dev.orion.twr.domain.model.User;
import dev.orion.twr.domain.port.in.ConversationUseCase;
import dev.orion.twr.domain.port.out.AuthPort;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource that exposes the authenticated (Orion Users) web conversation flow:
 * multiple named conversations per user, memory lookup and the streaming chatbot
 * endpoint used by the Vue frontend. Mirrors the contract of the RAG project's
 * {@code RagController}, adapted to the TWR domain.
 *
 * <p>All endpoints here require a valid Orion Users JWT (see
 * {@code mp.jwt.verify.*} in {@code application.properties} and {@link JwtAuthFilter}).
 * The legacy {@code /twr/chat} (phone-based) endpoint, used exclusively by the
 * WhatsApp channel, is untouched and lives in {@link TwrResource}.
 *
 * @author Rodrigo Prestes Machado
 */
@Path("/twr")
public class ConversationResource {

    /** Driving port used to manage and chat within conversations. */
    private final ConversationUseCase conversationUseCase;

    /** Port used to resolve the authenticated user from the request's JWT. */
    private final AuthPort authPort;

    /** JAX-RS request context, used to read the JWT stashed by {@link JwtAuthFilter}. */
    @Context
    ContainerRequestContext requestContext;

    /**
     * Creates the resource with its driving ports.
     *
     * @param conversationUseCase application port for conversation management and chat
     * @param authPort            port used to resolve the authenticated user from the JWT
     */
    @Inject
    public ConversationResource(ConversationUseCase conversationUseCase, AuthPort authPort) {
        this.conversationUseCase = conversationUseCase;
        this.authPort = authPort;
    }

    /**
     * Resolves the authenticated user from the JWT stored in the request context by
     * {@link JwtAuthFilter}.
     *
     * @return the authenticated user
     * @throws WebApplicationException with a 401 status if no JWT is present or invalid
     */
    private User authenticatedUser() {
        String jwtToken = (String) requestContext.getProperty("jwt.token");
        if (jwtToken == null) {
            throw new WebApplicationException("Token de autenticação não encontrado", Response.Status.UNAUTHORIZED);
        }
        try {
            return authPort.resolveUser(jwtToken);
        } catch (Exception e) {
            Log.warn("Failed to resolve user from JWT", e);
            throw new WebApplicationException("Token de autenticação inválido", Response.Status.UNAUTHORIZED);
        }
    }

    /**
     * Creates a new conversation for the authenticated user.
     *
     * @param userId  path variable (ignored; resolved from JWT)
     * @param request conversation creation request with title and specialist
     * @return the created conversation
     */
    @POST
    @Path("/users/{userId}/conversations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public Chat createConversation(@PathParam("userId") String userId, @Valid ConversationRequest request) {
        User user = authenticatedUser();
        TutorActivity activity = requiredActivity(request.activity);
        Log.info("Creating conversation for user: " + user.getOrionUserHash());
        return conversationUseCase.createConversation(user, request.title, activity);
    }

    /**
     * Returns all conversations belonging to the authenticated user.
     *
     * @param userId path variable (ignored; resolved from JWT)
     * @return the user's conversations
     */
    @GET
    @Path("/users/{userId}/conversations")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public List<Chat> getUserConversations(@PathParam("userId") String userId) {
        User user = authenticatedUser();
        Log.info("Getting conversations for user: " + user.getOrionUserHash());
        return conversationUseCase.listConversations(user.getOrionUserHash());
    }

    /**
     * Retrieves a conversation by its unique identifier.
     *
     * @param conversationId the conversation's unique identifier
     * @return the matching conversation
     */
    @GET
    @Path("/conversations/{conversationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public Chat getConversation(@PathParam("conversationId") String conversationId) {
        authenticatedUser();
        Log.info("Getting conversation: " + conversationId);
        return conversationUseCase.getConversation(conversationId)
                .orElseThrow(() -> new WebApplicationException("Conversa não encontrada", Response.Status.NOT_FOUND));
    }

    /**
     * Updates the title of a conversation owned by the authenticated user.
     *
     * @param conversationId conversation identifier
     * @param request        body with the new title
     * @return the updated conversation
     */
    @PATCH
    @Path("/conversations/{conversationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public Chat updateConversation(@PathParam("conversationId") String conversationId,
            @Valid ConversationRequest request) {
        User user = authenticatedUser();
        Log.info("Updating conversation title: " + conversationId);
        return handleOwnership(() ->
                conversationUseCase.renameConversation(conversationId, user.getOrionUserHash(), request.title));
    }

    /**
     * Stores the specialist of a conversation that does not have one yet.
     *
     * @param conversationId conversation identifier
     * @param request        body with {@code CONNECTIVES} or {@code EXPANSION}
     * @return the updated conversation
     */
    @PATCH
    @Path("/conversations/{conversationId}/activity")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public Chat assignActivity(@PathParam("conversationId") String conversationId,
            @Valid ActivityRequest request) {
        User user = authenticatedUser();
        TutorActivity activity = requiredActivity(request.activity);
        Log.info("Assigning activity " + activity + " to conversation " + conversationId);
        try {
            return handleOwnership(() ->
                    conversationUseCase.assignActivity(conversationId, user.getOrionUserHash(), activity));
        } catch (IllegalStateException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
        }
    }

    /**
     * Streams the first exercise of a conversation that has no messages yet.
     *
     * @param conversationId the conversation identifier
     * @return server-sent event stream of the first exercise
     */
    @POST
    @Path("/conversations/{conversationId}/exercise")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RolesAllowed("user")
    @Blocking
    public Multi<String> startExercise(@PathParam("conversationId") String conversationId) {
        User user;
        try {
            user = authenticatedUser();
        } catch (WebApplicationException e) {
            return Multi.createFrom().item("data: Erro: Token de autenticação não encontrado\n\n");
        }
        Log.info("Starting exercise for conversation: " + conversationId);
        try {
            return conversationUseCase.startExercise(user, conversationId)
                    .onFailure().recoverWithMulti(e -> Multi.createFrom().item(streamError(e)));
        } catch (SecurityException | NoSuchElementException | IllegalStateException e) {
            return Multi.createFrom().item(streamError(e));
        }
    }

    /**
     * Deletes a conversation owned by the authenticated user.
     *
     * @param conversationId the conversation to delete
     * @param userId         query param (used for logging only; authorisation is JWT-based)
     * @return HTTP 200 OK on success
     */
    @DELETE
    @Path("/conversations/{conversationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public Response deleteConversation(@PathParam("conversationId") String conversationId,
            @QueryParam("userId") String userId) {
        User user = authenticatedUser();
        Log.info("Deleting conversation " + conversationId + " by user " + user.getOrionUserHash());
        handleOwnership(() -> {
            conversationUseCase.deleteConversation(conversationId, user.getOrionUserHash());
            return null;
        });
        return Response.ok().build();
    }

    /**
     * Returns the message history of a conversation owned by the authenticated user.
     *
     * @param userId         query param (ignored; resolved from JWT)
     * @param conversationId the conversation identifier
     * @return the conversation memory, or {@code null} when parameters are missing
     */
    @GET
    @Path("/memory")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    @Blocking
    public MemoryResponse getMemory(@QueryParam("userId") String userId,
            @QueryParam("conversationId") String conversationId) {
        if (conversationId == null) {
            return null;
        }
        authenticatedUser();
        Log.info("Memory Conversation: " + conversationId);
        return conversationUseCase.getConversation(conversationId)
                .map(MemoryResponse::fromChat)
                .orElse(null);
    }

    /**
     * Streams a chatbot response for an authenticated user within a conversation.
     *
     * @param request the chatbot request containing the conversation ID and prompt
     * @return server-sent event stream of response tokens
     */
    @POST
    @Path("/chatbot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RolesAllowed("user")
    @Blocking
    public Multi<String> chatbot(@Valid ChatbotRequest request) {
        User user;
        try {
            user = authenticatedUser();
        } catch (WebApplicationException e) {
            return Multi.createFrom().item("data: Erro: Token de autenticação não encontrado\n\n");
        }
        Log.info("Chatbot POST - Conversation: " + request.conversationId);
        try {
            return conversationUseCase.chat(user, request.conversationId, request.prompt)
                    .onFailure().recoverWithMulti(e -> Multi.createFrom().item(streamError(e)));
        } catch (SecurityException | NoSuchElementException | IllegalStateException e) {
            return Multi.createFrom().item(streamError(e));
        }
    }

    /**
     * Parses a specialist name from the web client.
     *
     * @param raw {@code CONNECTIVES} or {@code EXPANSION}
     * @return the activity
     * @throws WebApplicationException with status 400 when the value is missing or unknown
     */
    private static TutorActivity requiredActivity(String raw) {
        try {
            TutorActivity activity = TutorActivity.fromApi(raw);
            if (activity == null) {
                throw new WebApplicationException("Choose connectives or expansion", Response.Status.BAD_REQUEST);
            }
            return activity;
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Formats a failure as a single SSE data payload, matching the chatbot error shape.
     *
     * @param error the failure
     * @return an SSE chunk
     */
    private static String streamError(Throwable error) {
        String msg = error instanceof SecurityException
                ? "Erro: Acesso negado à conversa"
                : "Erro: " + (error.getMessage() != null ? error.getMessage() : "Erro desconhecido");
        return "data: " + msg + "\n\n";
    }

    /**
     * Runs a conversation-owning operation, translating domain ownership failures into
     * the appropriate JAX-RS HTTP responses.
     *
     * @param <T>       the operation's result type
     * @param operation the operation to run
     * @return the operation's result
     */
    private <T> T handleOwnership(java.util.function.Supplier<T> operation) {
        try {
            return operation.get();
        } catch (NoSuchElementException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        } catch (SecurityException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.FORBIDDEN);
        }
    }

}
