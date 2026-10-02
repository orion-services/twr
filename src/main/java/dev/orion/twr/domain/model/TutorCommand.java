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

import java.text.Normalizer;

/**
 * WhatsApp commands that choose or switch the specialist agent.
 *
 * <p>The whole message must be the command. A command inside a longer answer is
 * not treated as a switch.
 *
 * @author Rodrigo Prestes Machado
 */
public final class TutorCommand {

    private TutorCommand() {
    }

    /**
     * Returns the activity named by a WhatsApp command, if the message is one.
     *
     * @param message the student message
     * @return the chosen activity, or {@code null} when the message is not a command
     */
    public static TutorActivity parse(String message) {
        if (message == null) {
            return null;
        }
        String normalized = Normalizer.normalize(message.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return switch (normalized) {
            case "\\connectives" -> TutorActivity.CONNECTIVES;
            case "\\expansion" -> TutorActivity.EXPANSION;
            default -> null;
        };
    }

}
