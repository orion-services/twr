/*
 * Copyright (c) 2026 Rodrigo Prestes Machado
 * All rights reserved.
 *
 * This source code is proprietary and confidential.
 * Unauthorized copying, modification, distribution, or use
 * of this software, via any medium, is strictly prohibited
 * without the express prior written permission of the copyright holder.
 */
package dev.rpmhub.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body that assigns a specialist to a conversation that does not have one yet.
 *
 * @author Rodrigo Prestes Machado
 */
public class ActivityRequest {

    /** {@code CONNECTIVES} or {@code EXPANSION}. */
    @NotBlank
    public String activity;

}
