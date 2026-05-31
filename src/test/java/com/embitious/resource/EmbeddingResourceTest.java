package com.embitious.resource;

import com.embitious.service.EmbeddingService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EmbeddingResourceTest {

    @Inject
    EmbeddingService embeddingService;

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
                .body("error", notNullValue());
    }

    @Test
    void testEmbedBatch() {
        List<String> texts = List.of(
                "The quick brown fox jumps over the lazy dog",
                "Artificial intelligence and deep learning",
                "Quarkus is a cloud-native Java framework"
        );
        List<float[]> embeddings = embeddingService.embedBatch(texts);
        assertNotNull(embeddings);
        assertEquals(3, embeddings.size());
        for (float[] emb : embeddings) {
            assertNotNull(emb);
            assertTrue(emb.length > 0);
        }
    }
}
