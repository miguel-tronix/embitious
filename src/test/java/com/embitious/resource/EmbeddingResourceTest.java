package com.embitious.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class EmbeddingResourceTest {

    @Test
    void testEmbedEndpoint() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"text\": \"Hello world\"}")
                .when()
                .post("/embed")
                .then()
                .statusCode(200)
                .body("embedding", notNullValue());
    }

    @Test
    void testEmbedEndpointEmptyText() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"text\": \"\"}")
                .when()
                .post("/embed")
                .then()
                .statusCode(400)
                .body(notNullValue());
    }
}
