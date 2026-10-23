package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.ApiError;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.amqp.AmqpException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.AuthenticationException;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {

        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        try {

            AuthResponse response = authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

            // Nếu không thể gửi message hoặc không xác nhận được việc gửi,
            // API trả HTTP 503.
        } catch (AmqpException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError(503, "Service unavailable",
                            "Cannot queue welcome email. Please try again later."));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            new ApiError(
                                    400,
                                    "Bad request",
                                    e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        try {

            return ResponseEntity.ok(
                    authService.login(request));

        } catch (AuthenticationException e) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED)
                    .body(
                            new ApiError(
                                    401,
                                    "Unauthorized",
                                    "Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {

        try {

            return ResponseEntity.ok(
                    authService.refresh(request));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED)
                    .body(
                            new ApiError(
                                    401,
                                    "Unauthorized",
                                    e.getMessage()));
        }
    }
}
