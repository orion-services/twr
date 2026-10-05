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

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Produces the {@link EmbeddingModel} CDI bean used for RAG in dev and test.
 *
 * <p>Uses the local, in-process all-MiniLM-L6-v2 ONNX model (384 dimensions), which
 * requires no external API and matches {@code quarkus.langchain4j.pgvector.dimension}
 * in those profiles. The {@code langchain4j-embeddings-all-minilm-l6-v2} dependency is
 * a plain library, not a Quarkus extension, so the bean must be produced explicitly.
 * Production does not include this bean: the image build selects the OpenAI embedding
 * model ({@code text-embedding-3-small}, 1536 dimensions) instead.
 *
 * @author Rodrigo Prestes Machado
 */
@IfBuildProfile(anyOf = { "dev", "test" })
@ApplicationScoped
public class EmbeddingModelProducer {

    /**
     * Produces the singleton embedding model instance.
     *
     * @return a local all-MiniLM-L6-v2 embedding model
     */
    @Produces
    @ApplicationScoped
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
