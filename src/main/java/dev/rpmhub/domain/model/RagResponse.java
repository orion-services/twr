/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.domain.model;

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
