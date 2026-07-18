package com.embitious.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Error payload returned when a request fails")
@RegisterForReflection
public class ErrorResponse {

    @Schema(description = "Machine-readable error code", example = "INVALID_INPUT")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "Text must not be null or empty")
    private String error;

    public ErrorResponse() {}

    public ErrorResponse(String errorCode, String error) {
        this.errorCode = errorCode;
        this.error = error;
    }

    public ErrorResponse(String error) {
        this.errorCode = "INTERNAL_ERROR";
        this.error = error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
