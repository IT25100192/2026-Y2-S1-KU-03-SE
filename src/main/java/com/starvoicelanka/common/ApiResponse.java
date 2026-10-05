package com.starvoicelanka.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Object details;

    public ApiResponse() {}

    public ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> success(T data, Object details) {
        ApiResponse<T> resp = new ApiResponse<>(true, data, null);
        resp.setDetails(details);
        return resp;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> resp = new ApiResponse<>(false, null, message);
        return resp;
    }

    public static <T> ApiResponse<T> error(String message, Object details) {
        ApiResponse<T> resp = new ApiResponse<>(false, null, message);
        resp.setDetails(details);
        return resp;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}
