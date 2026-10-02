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
package dev.orion.twr.domain.port.out;

import dev.orion.twr.domain.model.RagQuery;
import dev.orion.twr.domain.model.RagResponse;

/**
 * Driven port (out) for vector-similarity search.
 *
 * @author Rodrigo Prestes Machado
 */
public interface EmbeddingRepository {

    /**
     * Searches for relevant chunks based on the provided RAG query.
     *
     * @param query the RAG query containing the search parameters
     * @return the RAG response with the retrieved context passages
     */
    RagResponse searchChunks(RagQuery query);
}
