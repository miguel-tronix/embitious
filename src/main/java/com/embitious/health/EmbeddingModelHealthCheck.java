package com.embitious.health;

import com.embitious.service.EmbeddingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

/**
 * Custom readiness check that verifies the embedding model is loaded and can respond.
 * Failing this check will cause Kubernetes to stop routing traffic to this pod.
 */
@Readiness
@ApplicationScoped
public class EmbeddingModelHealthCheck implements HealthCheck {

    @Inject
    EmbeddingService embeddingService;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("embedding-model");
        try {
            // Perform a quick smoke test with a short, known text
            float[] result = embeddingService.embed("health check");
            if (result != null && result.length > 0) {
                return builder
                        .up()
                        .withData("model-dimensions", result.length)
                        .build();
            } else {
                return builder
                        .down()
                        .withData("reason", "Model returned null or empty embedding")
                        .build();
            }
        } catch (Exception e) {
            return builder
                    .down()
                    .withData("reason", "Model inference failed: " + e.getMessage())
                    .build();
        }
    }
}
