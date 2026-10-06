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

/**
 * Fixed texts sent to a specialist, or to the student, without asking the model
 * to choose the skill.
 *
 * @author Rodrigo Prestes Machado
 */
public final class TutorTexts {

    /**
     * Internal request that opens practice with the ATIVACAO greeting.
     * It is not stored as a student message.
     */
    public static final String FIRST_EXERCISE =
            "A conversa acabou de começar e o histórico está vazio. "
                    + "Execute o estado INICIO: envie exatamente o texto de ATIVACAO desta habilidade, "
                    + "sem acrescentar nada antes ou depois. Não apresente lacuna, exercício nem "
                    + "pergunta-guia ainda. Espere a resposta do aluno.";

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
            Envie \\connectives para ligar ideias.
            Envie \\expansion para acrescentar detalhes à frase.""";

    private TutorTexts() {
    }

}
