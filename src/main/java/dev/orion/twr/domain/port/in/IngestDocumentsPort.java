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

import java.util.List;

/**
 * Driving port (in) for ingesting documents from a local directory or scraped from URLs.
 *
 * @author Rodrigo Prestes Machado
 */
public interface IngestDocumentsPort {

    /**
     * Reads all supported documents from the given directory and ingests them into the embedding store.
     *
     * @param documentsPath file-system path to the directory containing the source documents
     */
    void execute(String documentsPath);

    /**
     * Scrapes the given URLs and ingests the resulting documents into the embedding store.
     *
     * @param urls URLs to scrape and ingest
     */
    void executeUrls(List<String> urls);
}
