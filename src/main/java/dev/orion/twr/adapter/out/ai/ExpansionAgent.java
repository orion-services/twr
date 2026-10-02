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
package dev.orion.twr.adapter.out.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Specialist that practices sentence expansion with one student conversation.
 *
 * @author Rodrigo Prestes Machado
 */
@RegisterAiService
@ApplicationScoped
public interface ExpansionAgent {

    /**
     * Streams an expansion-practice reply grounded in optional RAG context.
     *
     * @param memoryId conversation id on the web, phone number on WhatsApp
     * @param context  relevant passages retrieved from the vector store, possibly empty
     * @param prompt   the student message, or an internal request for the next exercise
     * @return a multi that emits the response chunks
     */
    @SystemMessage(fromResource = "/prompts/expansion.md")
    @UserMessage("Contexto: {context}\n\nPergunta: {prompt}")
    Multi<String> answer(@MemoryId String memoryId, String context, String prompt);

}
