/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.domain.port.out;

import dev.rpmhub.domain.model.User;

/**
 * Driven port that resolves the authenticated web user from an Orion Users JWT token.
 *
 * @author Rodrigo Prestes Machado
 */
public interface AuthPort {

    /**
     * Resolves the {@link User} identified by the given JWT token, decoding its claims.
     *
     * @param jwtToken the raw JWT token (without the {@code Bearer} prefix)
     * @return the resolved user, with {@code orionUserHash} and {@code email} populated
     */
    User resolveUser(String jwtToken);

}
