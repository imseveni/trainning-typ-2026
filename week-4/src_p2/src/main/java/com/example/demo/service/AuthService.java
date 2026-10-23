package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.messaging.WelcomeEmailPublisher;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final WelcomeEmailPublisher welcomeEmailPublisher;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            WelcomeEmailPublisher welcomeEmailPublisher) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.welcomeEmailPublisher = welcomeEmailPublisher;
    }

    // Method thực hiện chức năng đăng ký tài khoản.
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        validateRegister(request); // Kiểm tra username/password có hợp lệ hay không?

        // Kiểm tra username đã tồn tại chưa?
        if (userRepository.existsByUsername(request.username())) {

            throw new IllegalArgumentException("Username already exists");
        }

        // Nếu username chưa tồn tại, tạo AppUser
        AppUser user = new AppUser();

        // Gán username
        user.setUsername(request.username());

        // Gán email
        user.setEmail(request.email());

        // Hash password
        user.setPassword(
                passwordEncoder.encode(
                        request.password()));

        // Gán role mặc định
        user.setRole(Role.USER);

        // Lưu xuống database
        user = userRepository.save(user);

        // Tạo access token và refresh token.
        AuthResponse response = createTokenPair(user);

        // Gọi publisher gửi event.
        welcomeEmailPublisher.publish(user);

        // Trả kết quả về controller.
        return response;
    }

    // Method này xử lý đăng nhập.
    public AuthResponse login(LoginRequest request) {

        // Kiểm tra username/password này có đúng không.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        // Lấy đầy đủ entity AppUser để tạo token
        AppUser user = userRepository
                .findByUsername(request.username())
                .orElseThrow();

        // Sau khi login thành công, method này tạo token
        return createTokenPair(user);
    }

    // Kiểm tra request đăng ký
    private void validateRegister(RegisterRequest request) {

        if (request.username() == null || request.username().isBlank()) {

            throw new IllegalArgumentException(
                    "Username must not be blank");
        }

        if (request.password() == null || request.password().length() < 6) {

            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters");
        }
    }

    // Method này có nhiệm vụ tạo một cặp token sau khi user đăng ký hoặc đăng nhập
    // thành công, rồi đóng gói chúng vào AuthResponse để trả về client.
    private AuthResponse createTokenPair(AppUser user) {

        // Tạo Access Token
        String accessToken = jwtService.generateAccessToken(user);

        // Tạo Refresh Token
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.create(user);

        return new AuthResponse(
                "Bearer",
                accessToken,
                jwtService.getAccessExpirationMs(),
                refresh.token());
    }

    // Method này xử lý việc xin Access Token mới bằng Refresh Token.
    public AuthResponse refresh(RefreshRequest request) {

        if (request.refreshToken() == null || request.refreshToken().isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh token is required");
        }

        // Gọi rotate()
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(
                request.refreshToken());

        // Tạo Access Token mới
        String accessToken = jwtService.generateAccessToken(
                rotated.user());

        return new AuthResponse(
                "Bearer",
                accessToken,
                jwtService.getAccessExpirationMs(),
                rotated.token());
    }
}
