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
package dev.orion.twr.adapter.in.rest;

import java.io.IOException;
import java.io.InputStream;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Serves {@code index.html} for any client-side (Vue Router) route that has no matching
 * static asset or REST resource, so that deep links and page refreshes on routes like
 * {@code /login} or {@code /conversations} work instead of returning a raw 404.
 *
 * <p>Registered as a plain {@code {path:.*}} catch-all: JAX-RS resolves the more
 * specific, literal API routes ({@code /twr/*}, {@code /webhook/*}, {@code /q/*}) and the
 * static {@code /assets/*} handler ahead of this generic template regardless, so this
 * fallback only ever kicks in for genuinely unmatched paths (i.e. Vue Router routes like
 * {@code /login} or {@code /conversations}).
 *
 * @author Rodrigo Prestes Machado
 */
@Path("/{path:.*}")
public class SpaFallbackResource {

    /**
     * Returns the built SPA entry point.
     *
     * @return the {@code index.html} content, or a 404 if the frontend has not been built
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/resources/index.html")) {
            if (in == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(in.readAllBytes()).build();
        } catch (IOException e) {
            Log.error("Failed to read META-INF/resources/index.html for SPA fallback", e);
            return Response.serverError().build();
        }
    }

}
