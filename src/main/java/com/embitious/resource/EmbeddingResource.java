package com.embitious.resource;

import com.embitious.dto.EmbeddingRequest;
import com.embitious.dto.EmbeddingResponse;
import com.embitious.dto.ErrorResponse;
import com.embitious.service.EmbeddingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/embed")
@Tag(name = "Embeddings", description = "Text embedding operations")
public class EmbeddingResource {

    private static final Logger LOG = Logger.getLogger(EmbeddingResource.class);

    @Inject
    EmbeddingService embeddingService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Compute text embedding", description = "Computes a vector embedding for the given input text using a Hugging Face sentence-transformers model")
    @APIResponse(responseCode = "200", description = "Embedding computed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EmbeddingResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid input — text is null or empty",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal server error during embedding computation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response embed(
            @RequestBody(description = "The request containing text to embed", required = true,
                    content = @Content(schema = @Schema(implementation = EmbeddingRequest.class)))
            EmbeddingRequest request) {
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
}
