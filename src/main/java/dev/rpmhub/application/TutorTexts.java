/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.application;

/**
 * Fixed texts sent to a specialist, or to the student, without asking the model
 * to choose the skill.
 *
 * @author Rodrigo Prestes Machado
 */
public final class TutorTexts {

    /**
     * Internal request that opens practice. It is not stored as a student message.
     */
    public static final String FIRST_EXERCISE =
            "Apresente o primeiro exercício desta habilidade: uma frase avulsa com uma lacuna. "
                    + "Uma questão apenas. Não entregue a resposta.";

    /**
     * Internal request after a WhatsApp agent switch. It is not stored as a student message.
     */
    public static final String NEXT_EXERCISE =
            "Apresente o próximo exercício desta habilidade, somente da habilidade deste professor. "
                    + "Uma questão apenas. Não entregue a resposta.";

    /**
     * WhatsApp reply when the current conversation has no specialist yet.
     */
    public static final String WHATSAPP_CHOICE = """
            Que tipo de exercício você deseja fazer?
            Envie \\conectivos para ligar ideias.
            Envie \\expansao para acrescentar detalhes à frase.""";

    private TutorTexts() {
    }

}
