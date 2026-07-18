package com.embitious.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EmbeddingResponse", description = "Response containing the computed embedding vector")
@RegisterForReflection
public class EmbeddingResponse {

    @Schema(description = "The embedding vector as an array of floats", example = "[0.0123, -0.0456, 0.0789, ...]")
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
