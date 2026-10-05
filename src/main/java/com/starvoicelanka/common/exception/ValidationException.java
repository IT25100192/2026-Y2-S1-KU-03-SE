package com.starvoicelanka.common.exception;

public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(400, message);
    }

    public ValidationException(String message, Object details) {
        super(400, message, details);
    }
}
