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
package dev.orion.twr.domain.model;

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
