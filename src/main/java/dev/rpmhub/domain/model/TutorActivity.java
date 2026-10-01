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

/**
 * Skill practiced in a conversation, and therefore which specialist agent answers it.
 *
 * @author Rodrigo Prestes Machado
 */
public enum TutorActivity {

    /** Textual connectives: cause, contrast, addition, conclusion. */
    CONNECTIVES,

    /** Sentence expansion: who, how, when, where, why. */
    EXPANSION;

    /**
     * Parses the value stored or sent by the web client.
     *
     * @param raw {@code CONNECTIVES} or {@code EXPANSION}, ignoring case
     * @return the activity, or {@code null} when {@code raw} is blank
     * @throws IllegalArgumentException when the value is not a known activity
     */
    public static TutorActivity fromApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TutorActivity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Habilidade desconhecida: " + raw);
        }
    }

}
