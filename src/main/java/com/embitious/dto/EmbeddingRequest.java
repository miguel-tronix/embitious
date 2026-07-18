package com.embitious.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EmbeddingRequest", description = "Request payload containing the text to embed")
@RegisterForReflection
public class EmbeddingRequest {

    @NotBlank(message = "Text must not be null or empty")
    @Schema(description = "The input text to compute an embedding vector for", required = true, example = "Hello world")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
