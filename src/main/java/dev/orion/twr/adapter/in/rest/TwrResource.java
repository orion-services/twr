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

import dev.orion.twr.domain.port.in.ChatUseCase;
import dev.orion.twr.domain.validation.BrazilianPhoneValidator;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource that exposes the TWR chat endpoint.
 *
 * @author Rodrigo Prestes Machado
 */
@Path("/twr")
public class TwrResource {

    /**
     * Driving port used to process chat requests.
     */
    private final ChatUseCase chatUseCase;

    /**
     * Creates the REST resource with the chat use case.
     *
     * @param chatUseCase application port for chatting
     */
    @Inject
    public TwrResource(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    /**
     * Streams a chat reply for the submitted user message.
     *
     * @param phoneNumber phone number that identifies the user
     * @param message the user message submitted as form data
     * @return a multi that emits the response as plain text chunks
     * @throws WebApplicationException with a 400 status if {@code phoneNumber} is not
     *         a valid Brazilian phone number
     */
    @POST
    @Path("/chat")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public Multi<String> chat(
            @FormParam("phoneNumber") String phoneNumber,
            @FormParam("message") String message) {
        String normalizedPhoneNumber = BrazilianPhoneValidator.normalize(phoneNumber);
        if (normalizedPhoneNumber == null) {
            throw new WebApplicationException(
                    "Informe um telefone válido para o Brasil, com DDD.",
                    Response.Status.BAD_REQUEST);
        }
        return chatUseCase.chat(normalizedPhoneNumber, message);
    }

}
