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
            case "\\conectivos" -> TutorActivity.CONNECTIVES;
            case "\\expansao" -> TutorActivity.EXPANSION;
            default -> null;
        };
    }

}
