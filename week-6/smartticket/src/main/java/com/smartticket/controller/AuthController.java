package com.smartticket.controller;

import com.smartticket.dto.AuthResponse;
import com.smartticket.dto.AuthUserResponse;
import com.smartticket.dto.LoginRequest;
import com.smartticket.dto.RegisterRequest;
import com.smartticket.enums.Role;
import com.smartticket.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        AuthUserResponse user = authService.getCurrentUser(userId(jwt));
        Role role = jwt.getClaimAsStringList("roles").stream()
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .map(Role::valueOf)
                .findFirst()
                .orElse(Role.USER);

        return new AuthUserResponse(
                user.userId(), user.fullName(), user.email(), role);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
