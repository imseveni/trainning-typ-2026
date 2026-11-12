package com.smartticket.service;

import com.smartticket.dto.AuthResponse;
import com.smartticket.dto.AuthUserResponse;
import com.smartticket.dto.LoginRequest;
import com.smartticket.dto.RegisterRequest;
import com.smartticket.enums.Role;
import com.smartticket.model.User;
import com.smartticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    @Value("${app.jwt.expiration-minutes:30}")
    private long expirationMinutes;

    // Đăng ký
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Role được gán phía server; không nhận quyền do client gửi lên.
        user.setRole(Role.USER);

        try {
            return issueToken(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Bảo vệ cả trường hợp hai request đăng ký cùng email gần như đồng thời.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    // Đăng nhập
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return issueToken(user);
    }

    // Lấy thông tin tài khoản
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        return toUserResponse(user);
    }

    // Tạo JWT
    private AuthResponse issueToken(User user) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);

        String roleAuthority = "ROLE_" + user.getRole().name();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("roles", java.util.List.of(roleAuthority))
                .build();

        // Khớp thuật toán trong header của token với khóa HMAC đã cấu hình.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new AuthResponse(token, "Bearer", expiresAt, toUserResponse(user));
    }

    private AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(
                user.getUserId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
    }
}
