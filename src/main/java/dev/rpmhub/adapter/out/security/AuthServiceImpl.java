/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.adapter.out.security;

import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rpmhub.domain.model.User;
import dev.rpmhub.domain.port.out.AuthPort;
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
