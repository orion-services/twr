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
 * Domain model that represents a chat user.
 *
 * @author Rodrigo Prestes Machado
 */
public class User {

    /**
     * Phone number that identifies the user (WhatsApp channel).
     */
    private String phoneNumber;

    /**
     * Stable hash that identifies the user in the Orion Users service (web channel,
     * extracted from the {@code c_hash} JWT claim).
     */
    private String orionUserHash;

    /**
     * Email address of the user, as reported by the Orion Users JWT ({@code email} claim).
     */
    private String email;

    /**
     * Returns the user phone number.
     *
     * @return the phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the user phone number.
     *
     * @param phoneNumber the phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the Orion Users hash that identifies this user on the web channel.
     *
     * @return the Orion Users hash
     */
    public String getOrionUserHash() {
        return orionUserHash;
    }

    /**
     * Sets the Orion Users hash that identifies this user on the web channel.
     *
     * @param orionUserHash the Orion Users hash to set
     */
    public void setOrionUserHash(String orionUserHash) {
        this.orionUserHash = orionUserHash;
    }

    /**
     * Returns the user email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user email address.
     *
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

}
