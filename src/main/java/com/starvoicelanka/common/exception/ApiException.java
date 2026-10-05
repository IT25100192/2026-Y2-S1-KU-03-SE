package com.starvoicelanka.common.exception;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final Object details;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.details = null;
    }

    public ApiException(int statusCode, String message, Object details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details;
    }

    public int getStatusCode() { return statusCode; }
    public Object getDetails() { return details; }
}
