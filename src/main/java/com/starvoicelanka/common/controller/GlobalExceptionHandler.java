package com.starvoicelanka.common.controller;

import com.starvoicelanka.common.ApiResponse;
import com.starvoicelanka.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }

    @ExceptionHandler(ApiException.class)
    public Object handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API Exception [{}]: {}", ex.getStatusCode(), ex.getMessage());
        if (isApiRequest(request)) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage(), ex.getDetails()));
        }

        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.resolve(ex.getStatusCode()) != null ? HttpStatus.resolve(ex.getStatusCode()) : HttpStatus.BAD_REQUEST);
        mav.addObject("title", ex.getStatusCode() == 404 ? "Not found" : "Error");
        mav.addObject("status", ex.getStatusCode());
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Validation failed", errors));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("title", "Validation Error");
        mav.addObject("status", 400);
        mav.addObject("message", "Validation failed: " + errors.values().iterator().next());
        return mav;
    }

    /** Shared by the handlers below: a bad or missing value in a form or request is a 400, not a 500. */
    private Object badInput(String message, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("title", "Check your input");
        mav.addObject("status", 400);
        mav.addObject("message", message);
        return mav;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return badInput("Please fill in the required field: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return badInput("The value for '" + ex.getName() + "' is not valid. Check it and try again.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return badInput("The request body is missing or not valid JSON", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUploadTooBig(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return badInput("That file is too large to upload", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
        // This exception covers two different situations that need different
        // wording: a UNIQUE constraint (e.g. duplicate email) and a FOREIGN KEY
        // constraint (e.g. trying to delete a record something else still
        // references). Guessing wrong here is worse than a generic message.
        String reason = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        String message;
        if (reason != null && reason.toLowerCase().contains("foreign key")) {
            message = "This can't be deleted because other records still depend on it";
        } else {
            message = "That conflicts with an existing record (e.g. a value that must be unique)";
        }
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.CONFLICT);
        mav.addObject("title", "Conflict");
        mav.addObject("status", 409);
        mav.addObject("message", message);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Something went wrong on our side"));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("title", "Something went wrong");
        mav.addObject("status", 500);
        mav.addObject("message", ex.getMessage() != null ? ex.getMessage() : "Unexpected error.");
        return mav;
    }
}
