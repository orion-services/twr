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

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Extracts plain text from PDF files using Apache PDFBox for use in the RAG ingestion pipeline.
 *
 * @author Rodrigo Prestes Machado
 */
@ApplicationScoped
public class PDFExtractor {

    /**
     * Extracts text from a PDF file located at the given path.
     *
     * @param path the path to the PDF file
     * @return the extracted text as a String
     */
    public String extractText(Path path) {
        String text = "";
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(document);
            } else {
                Log.error("The document is encrypted.");
            }
        } catch (IOException e) {
            Log.error("Could not read the file.");
        }
        return text;
    }

    /**
     * Checks if the given file path points to a PDF file.
     *
     * @param filePath the path to the file
     * @return true if the file is a PDF, false otherwise
     */
    public boolean isPdfFile(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".pdf");
    }

}
