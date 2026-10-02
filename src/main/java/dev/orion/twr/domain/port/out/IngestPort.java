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

import dev.orion.twr.domain.model.DocumentData;

import java.util.List;

/**
 * Driven port (out) for document ingestion into the embedding store.
 *
 * @author Rodrigo Prestes Machado
 */
public interface IngestPort {

    /**
     * Ingests documents from the specified directory.
     *
     * @param directoryPath the path to the directory containing the documents
     */
    void ingestDocuments(String directoryPath);

    /**
     * Ingests the given documents into the embedding store (chunking + embedding + persist).
     *
     * @param documents the list of documents to ingest
     */
    void ingestDocuments(List<DocumentData> documents);
}
