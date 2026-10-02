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
package dev.orion.twr.adapter.config;

import dev.orion.twr.domain.port.in.IngestDocumentsPort;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Runs local-directory document ingestion (using the configured {@code rag.location}) and,
 * when configured, URL scraping ingestion (using {@code rag.scrape.urls}) at startup.
 *
 * @author Rodrigo Prestes Machado
 */
@ApplicationScoped
public class IngestDocumentsStartup {

    /** Port used to trigger the document-ingestion use case. */
    private final IngestDocumentsPort ingestDocumentsPort;

    /** File-system path to the directory containing the base documents to ingest. */
    @ConfigProperty(name = "rag.location")
    Path documentsPath;

    /** URLs to scrape and ingest at startup, if configured. */
    @ConfigProperty(name = "rag.scrape.urls")
    Optional<List<String>> scrapeUrls;

    /**
     * Constructs the observer with the required ingestion port.
     *
     * @param ingestDocumentsPort port that orchestrates document ingestion
     */
    @Inject
    public IngestDocumentsStartup(IngestDocumentsPort ingestDocumentsPort) {
        this.ingestDocumentsPort = ingestDocumentsPort;
    }

    /**
     * Triggers document ingestion from the configured directory and, if any URLs are
     * configured, scrapes and ingests them too, when the application starts.
     *
     * @param event CDI startup event (unused, only signals application readiness)
     */
    void onStartup(@Observes StartupEvent event) {
        try {
            ingestDocumentsPort.execute(documentsPath.toString());
        } catch (Exception e) {
            Log.error("Error ingesting documents at startup", e);
        }

        List<String> urls = scrapeUrls.orElse(List.of());
        if (!urls.isEmpty()) {
            try {
                ingestDocumentsPort.executeUrls(urls);
            } catch (Exception e) {
                Log.error("Error scraping and ingesting URLs at startup", e);
            }
        }
    }
}
