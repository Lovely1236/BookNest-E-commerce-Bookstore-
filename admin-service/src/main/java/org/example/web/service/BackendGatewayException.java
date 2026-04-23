package org.example.web.service;

public class BackendGatewayException extends RuntimeException {
    public BackendGatewayException(String message) { super(message); }
    public BackendGatewayException(String message, Throwable cause) { super(message, cause); }
}
