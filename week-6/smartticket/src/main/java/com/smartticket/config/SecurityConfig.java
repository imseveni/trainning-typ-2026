package com.smartticket.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.core.convert.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // Tạo khóa bí mật dùng cho JWT
    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String base64Secret) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT HMAC secret must contain at least 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // Cấu hình bộ ký token
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        // Chỉ rõ thuật toán HMAC mà khóa bí mật này hỗ trợ để encoder
        // chọn đúng JWK khi ký access token.
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    // Cấu hình bộ xác minh token
    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${app.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    // Bộ mã hóa mật khẩu
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Đổi role trong JWT thành quyền Spring Security
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Hỗ trợ cả token cũ ghi "ADMIN" và token mới ghi "ROLE_ADMIN".
        // Cả hai đều được chuẩn hóa về ROLE_ADMIN để khớp với hasRole("ADMIN").
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            Object rolesClaim = jwt.getClaims().get("roles");
            if (!(rolesClaim instanceof Collection<?> roles)) {
                return List.of();
            }

            return roles.stream()
                    .map(String::valueOf)
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .map(authority -> (GrantedAuthority) authority)
                    .toList();
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // Cấu hình chuỗi filter bảo mật
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        // Dùng chung handler cho authorization và resource-server. Nếu JWT hợp lệ
        // nhưng một rule từ chối request, log này sẽ cho biết request/principal cụ thể.
        AccessDeniedHandler diagnosticAccessDeniedHandler = (request, response, exception) -> {
            Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();
            String principal = authentication == null
                    ? "anonymous"
                    : authentication.getName();
            Object authorities = authentication == null
                    ? List.of()
                    : authentication.getAuthorities();

            log.warn("SECURITY_403 method={} uri={} server={}:{} principal={} authorities={} cause={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getServerName(),
                    request.getServerPort(),
                    principal,
                    authorities,
                    exception.getClass().getSimpleName());
            response.sendError(HttpStatus.FORBIDDEN.value());
        };

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phép Spring thực hiện error dispatch tới /error;
                        // nếu không, denyAll có thể che lỗi ban đầu thành HTTP 403.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/", "/index.html", "/styles.css", "/readability.css",
                                "/app.js", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/events/**", "/api/showtimes/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/**", "/api/showtimes/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**", "/api/showtimes/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**", "/api/showtimes/**")
                        .hasRole("ADMIN")
                        // Endpoint POST gốc là /api/holds (không có path con);
                        // khai báo tường minh để mọi user đã đăng nhập đều được giữ ghế.
                        .requestMatchers("/api/holds", "/api/holds/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(diagnosticAccessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .accessDeniedHandler(diagnosticAccessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
