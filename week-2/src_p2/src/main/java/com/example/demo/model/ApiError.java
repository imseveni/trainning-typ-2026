package com.example.demo.model;

public class ApiError {

    private int code;
    private String message;
    private String details;

    public ApiError(int code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}