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
package dev.orion.twr.application;

import java.util.ArrayList;
import java.util.List;

import dev.orion.twr.domain.model.DocumentData;
import dev.orion.twr.domain.port.in.IngestDocumentsPort;
import dev.orion.twr.domain.port.out.IngestPort;
import dev.orion.twr.domain.port.out.WebScraperPort;
import io.quarkus.logging.Log;

/**
 * Application service for ingesting local documents and scraped URLs into the embedding
 * repository.
 *
 * <p>Plain Java — instantiated by the Composition Root
 * ({@code dev.orion.twr.adapter.config.ApplicationBeans}).
 *
 * @author Rodrigo Prestes Machado
 */
public class IngestService implements IngestDocumentsPort {

    /** Repository used to store embedded document chunks in the vector store. */
    private final IngestPort ingestPort;

    /** Port used to fetch and convert URLs to Markdown documents before ingestion. */
    private final WebScraperPort webScraperPort;

    /**
     * Creates an IngestService with the given ingestion and scraping ports.
     *
     * @param ingestPort     repository for document ingestion and storage
     * @param webScraperPort port used to scrape URLs into ingestible documents
     */
    public IngestService(IngestPort ingestPort, WebScraperPort webScraperPort) {
        this.ingestPort = ingestPort;
        this.webScraperPort = webScraperPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(String documentsPath) {
        try {
            ingestPort.ingestDocuments(documentsPath);
            Log.info("📂 Base documents from '" + documentsPath + "' ingested successfully");
        } catch (Exception e) {
            Log.error("Error ingesting documents", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        webScraperPort.clearMarkdownOutputDirIfEnabled();

        List<DocumentData> documents = new ArrayList<>();
        for (String url : urls) {
            webScraperPort.scrapeToDocument(url).ifPresent(documents::add);
        }

        try {
            ingestPort.ingestDocuments(documents);
            Log.info("🌐 Scraped documents from " + documents.size() + "/" + urls.size()
                    + " URL(s) ingested successfully");
        } catch (Exception e) {
            Log.error("Error ingesting scraped URLs", e);
        }
    }
}
