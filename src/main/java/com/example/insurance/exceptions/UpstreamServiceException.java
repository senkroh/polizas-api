package com.example.insurance.exceptions;

// Class for when the WireMock API upstream fails
public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
