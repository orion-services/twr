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
package dev.orion.twr.domain.port.in;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Driving port for chatting with TWR.
 *
 * @author Rodrigo Prestes Machado
 */
public interface ChatUseCase {

    /**
     * Accepts a user message, assigns it to a chat session, and streams a reply.
     *
     * @param phoneNumber phone number that identifies the user
     * @param message text content sent by the user
     * @return a multi that emits the assistant response as plain text chunks
     */
    Multi<String> chat(String phoneNumber, String message);

    /**
     * Convenience variant of {@link #chat(String, String)} for callers that need the
     * complete assistant reply as a single value instead of a stream (e.g. WhatsApp,
     * which cannot deliver partial/streamed messages). Reuses the same streaming
     * pipeline, so RAG retrieval, persistence and memory behave identically.
     *
     * @param phoneNumber phone number that identifies the user
     * @param message text content sent by the user
     * @return a uni that emits the full assistant response once streaming completes
     */
    default Uni<String> chatSync(String phoneNumber, String message) {
        return chat(phoneNumber, message)
                .collect().in(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString);
    }

}
