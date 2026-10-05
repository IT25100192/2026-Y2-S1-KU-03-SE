package com.starvoicelanka.common.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(400, message);
    }

    public BadRequestException(String message, Object details) {
        super(400, message, details);
    }
}
