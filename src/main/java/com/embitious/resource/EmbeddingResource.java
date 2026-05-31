package com.embitious.resource;

import com.embitious.service.EmbeddingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/embed")
public class EmbeddingResource {

    private static final Logger LOG = Logger.getLogger(EmbeddingResource.class);

    @Inject
    EmbeddingService embeddingService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response embed(EmbeddingRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Text must not be null or empty"))
                    .build();
        }

        try {
            long startTime = System.currentTimeMillis();
            float[] embedding = embeddingService.embed(request.getText());
            long duration = System.currentTimeMillis() - startTime;
            LOG.debugf("Computed embedding in %d ms", duration);
            return Response.ok(new EmbeddingResponse(embedding)).build();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to compute embedding for text: %s", request.getText());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Embedding computation failed: " + e.getMessage()))
                    .build();
        }
    }

    public static class EmbeddingRequest {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class EmbeddingResponse {
        private float[] embedding;

        public EmbeddingResponse() {}

        public EmbeddingResponse(float[] embedding) {
            this.embedding = embedding;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {}

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
