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
package dev.orion.twr.domain.model;

import java.util.List;

/**
 * Domain representation of a RAG response — pure Java, no framework annotations.
 *
 * @author Rodrigo Prestes Machado
 */
public class RagResponse {

    /** Ordered list of context passages retrieved from the embedding store. */
    private final List<String> contexts;

    /**
     * Creates a RagResponse with the retrieved contexts.
     *
     * @param contexts list of retrieved context passages, best match first
     */
    public RagResponse(List<String> contexts) {
        this.contexts = contexts;
    }

    public List<String> getContexts() {
        return contexts;
    }
}
