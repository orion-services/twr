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
package dev.orion.twr.domain.port.out;

/**
 * Driven port (out) for sending outbound WhatsApp messages and feedback signals.
 *
 * @author Rodrigo Prestes Machado
 */
public interface MessageSenderPort {

    /**
     * Sends a text message to a WhatsApp user.
     *
     * @param to              recipient phone number in international format (no {@code +})
     * @param text            message content
     * @param overridePhoneId business phone number ID from the webhook metadata (or {@code null} to use the configured one)
     * @return {@code true} if the message was sent successfully
     */
    boolean sendTextMessage(String to, String text, String overridePhoneId);

    /**
     * Sends a read receipt plus a visual "processing" signal for an incoming message.
     *
     * @param to              recipient phone number in international format (no {@code +})
     * @param messageId       WhatsApp message ID (WAMID) being acknowledged
     * @param overridePhoneId business phone number ID from the webhook metadata (or {@code null} to use the configured one)
     * @return {@code true} if at least one of the signals was sent successfully
     */
    boolean sendTypingIndicator(String to, String messageId, String overridePhoneId);

    /**
     * Sends (or clears, with an empty emoji) an emoji reaction to a message.
     *
     * @param to              recipient phone number in international format (no {@code +})
     * @param messageId       WhatsApp message ID (WAMID) to react to
     * @param emoji           emoji to send, or {@code ""} to clear a previous reaction
     * @param overridePhoneId business phone number ID from the webhook metadata (or {@code null} to use the configured one)
     * @return {@code true} if the reaction was sent successfully
     */
    boolean sendReaction(String to, String messageId, String emoji, String overridePhoneId);
}
