package com.embitious.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Error payload returned when a request fails")
public class ErrorResponse {

    @Schema(description = "Human-readable error message", example = "Text must not be null or empty")
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
