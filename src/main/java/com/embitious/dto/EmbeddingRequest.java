package com.embitious.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EmbeddingRequest", description = "Request payload containing the text to embed")
public class EmbeddingRequest {

    @Schema(description = "The input text to compute an embedding vector for", required = true, example = "Hello world")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
