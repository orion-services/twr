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
package dev.orion.twr.adapter.out.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.orion.twr.domain.model.RagQuery;
import dev.orion.twr.domain.model.RagResponse;
import dev.orion.twr.domain.port.out.EmbeddingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of the {@link EmbeddingRepository} port using LangChain4j, backed by the
 * pgvector-based {@link EmbeddingStore} and a local embedding model.
 *
 * @author Rodrigo Prestes Machado
 */
@ApplicationScoped
public class RagRetriever implements EmbeddingRepository {

    /** LangChain4j store that holds vectorised text segments. */
    private final EmbeddingStore<TextSegment> embeddingStore;
    /** Model used to vectorise queries before similarity search. */
    private final EmbeddingModel embeddingModel;

    /**
     * Creates a RagRetriever with all required collaborators.
     *
     * @param embeddingStore LangChain4j store holding vectorised text segments
     * @param embeddingModel model used to embed queries before similarity search
     */
    @Inject
    public RagRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RagResponse searchChunks(RagQuery query) {
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(query.getQuery()).content())
                .minScore(query.getMinScore())
                .maxResults(query.getMaxResults())
                .build();

        var matches = embeddingStore.search(searchRequest).matches();
        var contexts = matches.stream()
                .map(match -> match.embedded().text())
                .toList();

        return new RagResponse(contexts);
    }
}
