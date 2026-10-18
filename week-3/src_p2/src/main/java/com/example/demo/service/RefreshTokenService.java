package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.RefreshToken;
import com.example.demo.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.time.Instant;

import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    // Dùng để tạo refresh token ngẫu nhiên.
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.refresh-token.expiration-ms}")
    private long expirationMs;

    public RefreshTokenService(RefreshTokenRepository repository) {

        this.repository = repository;
    }

    // Tạo một refresh token mới cho user.
    @Transactional // Nghĩa là toàn bộ thao tác database trong method này nằm trong một
                   // transaction.
    public IssuedRefreshToken create(AppUser user) {

        byte[] bytes = new byte[64];

        secureRandom.nextBytes(bytes);

        String rawToken = Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();

        // Hash refresh token
        refreshToken.setTokenHash(hash(rawToken));

        // Gắn token với user
        refreshToken.setUser(user);

        // Thiết lập thời gian hết hạn
        refreshToken.setExpiresAt(Instant.now().plusMillis(expirationMs));

        // Thiết lập token mới chưa bị thu hồi
        refreshToken.setRevoked(false);

        repository.save(refreshToken);

        return new IssuedRefreshToken(rawToken, refreshToken.getExpiresAt());
    }

    // Method này có nhiệm vụ kiểm tra token cũ rồi đổi sang token mới.
    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {

        // Tìm token trong DB
        RefreshToken current = repository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid refresh token"));

        // Kiểm tra token đã bị revoke chưa?
        if (current.isRevoked()) {

            throw new IllegalArgumentException(
                    "Refresh token has been revoked");
        }

        // Kiểm tra token hết hạn hay chưa?
        if (current.getExpiresAt().isBefore(Instant.now())) {

            throw new IllegalArgumentException(
                    "Refresh token has expired");
        }

        // Thu hồi token cũ
        current.setRevoked(true);

        repository.save(current);

        // Tạo token mới
        IssuedRefreshToken next = create(current.getUser());

        return new RotatedRefreshToken(
                current.getUser(),
                next.token(),
                next.expiresAt());
    }

    // Method có nhiệm vụ chuyển refresh token thật thành SHA-256 hash.
    private String hash(String rawToken) {

        try {

            // Tạo một object dùng thuật toán SHA-256
            MessageDigest digest = MessageDigest
                    .getInstance("SHA-256");

            // Hash
            byte[] bytes = digest.digest(
                    // Chuyển String thành byte[]
                    rawToken.getBytes(StandardCharsets.UTF_8));

            // Chuyển hash sang Hex
            return HexFormat.of().formatHex(bytes);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(e);
        }
    }

    // Đây là một record dùng để trả kết quả khi tạo token mới.
    public record IssuedRefreshToken(
            String token,
            Instant expiresAt) {
    }

    // Đây là một record dùng để trả kết quả khi dùng rotate.
    public record RotatedRefreshToken(
            AppUser user,
            String token,
            Instant expiresAt) {
    }
}