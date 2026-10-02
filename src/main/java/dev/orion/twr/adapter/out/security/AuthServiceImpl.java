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
package dev.orion.twr.adapter.out.security;

import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.orion.twr.domain.model.User;
import dev.orion.twr.domain.port.out.AuthPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adapter that decodes Orion Users JWT tokens without a full cryptographic re-check
 * (signature/issuer are already verified upstream by SmallRye JWT / {@code JwtAuthFilter}
 * before this class is invoked). Ownership of web conversations is denormalized directly
 * on the chat row (see {@code ChatEntity}), so no separate local users table is needed.
 *
 * @author Rodrigo Prestes Machado
 */
@ApplicationScoped
public class AuthServiceImpl implements AuthPort {

    /** Jackson mapper used to parse the JWT payload. */
    private final ObjectMapper objectMapper;

    /**
     * Creates the auth service.
     *
     * @param objectMapper Jackson mapper used to parse the JWT payload
     */
    @Inject
    public AuthServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public User resolveUser(String jwtToken) {
        JsonNode claims = decodePayload(jwtToken);
        User user = new User();
        if (claims.has("c_hash")) {
            user.setOrionUserHash(claims.get("c_hash").asText());
        }
        if (claims.has("email")) {
            user.setEmail(claims.get("email").asText());
        }
        if (user.getOrionUserHash() == null) {
            throw new IllegalArgumentException(
                    "Hash not found in JWT token. Available claims: " + claims.fieldNames());
        }
        return user;
    }

    /**
     * Decodes the (already-verified) JWT payload segment into a JSON tree.
     *
     * @param jwtToken the raw JWT token
     * @return the decoded claims
     */
    private JsonNode decodePayload(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readTree(payload);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode JWT token", e);
        }
    }

}
