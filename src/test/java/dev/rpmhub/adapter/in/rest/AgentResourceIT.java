package dev.rpmhub.adapter.in.rest;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Integration tests for the packaged application.
 *
 * @author Rodrigo Prestes Machado
 */
@QuarkusIntegrationTest
class AgentResourceIT {

    /**
     * Ensures the packaged app exposes the chat resource (GET is not allowed).
     */
    @Test
    void chatEndpointIsRegistered() {
        given()
                .when().get("/dora/chat")
                .then()
                .statusCode(405);
    }

}
