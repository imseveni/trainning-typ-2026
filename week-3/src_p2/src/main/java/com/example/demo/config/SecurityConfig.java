package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { // Tạo một PasswordEncoder sử dụng BCrypt.

        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager( // Method này tạo AuthenticationManager được dùng chủ yếu lúc
                                                        // login bằng username/password.
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        // Chịu trách nhiệm xác thực username/password bằng dữ liệu lấy từ database.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        // Kiểm tra password người dùng vừa nhập có đúng với password hash trong
        // database không?
        provider.setPasswordEncoder(passwordEncoder);

        // Tạo một AuthenticationManager và cho nó sử dụng DaoAuthenticationProvider
        // để xác thực.
        return new ProviderManager(provider);
    }

    @Bean
    // Tạo và cấu hình chuỗi các Security Filter mà mọi HTTP request sẽ đi qua trước
    // khi tới Controller.
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, // HttpSecurity dùng để cấu hình Spring Security.
            JwtAuthenticationFilter jwtFilter)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF
                                              // CSRF thường liên quan nhiều đến authentication dựa trên browser
                                              // cookie/session.

                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth // Endpoint này được ai truy cập?

                        .requestMatchers(
                                "/auth/**")
                        .permitAll() // Cho phép tất cả

                        .requestMatchers(
                                HttpMethod.GET,
                                "/students/**",
                                "/departments/**")
                        .hasAnyRole(
                                "USER",
                                "ADMIN")

                        .requestMatchers(
                                "/students/**",
                                "/departments/**")
                        .hasRole("ADMIN")

                        .anyRequest().authenticated()) // Những endpoint không match các rule phía trên thì ít nhất phải
                                                       // đăng nhập. Nhưng không yêu cầu cụ thể USER hay ADMIN.

                .exceptionHandling(ex -> ex // Cấu hình response khi Security từ chối request.

                        .authenticationEntryPoint( // Dùng khi user chưa được xác thực nhưng cố truy cập API cần
                                                   // authentication.
                                (request, response, exception) -> {

                                    response.setStatus(401);
                                    response.setContentType(
                                            "application/json"); // Response body là JSON.

                                    response.getWriter().write(
                                            """
                                                    {
                                                      "code": 401,
                                                      "message": "Unauthorized",
                                                      "details": "Authentication required"
                                                    }
                                                    """);
                                })

                        .accessDeniedHandler( // Dùng khi User đã Authentication thành công nhưng không có đủ quyền.
                                (request, response, exception) -> {

                                    response.setStatus(403);
                                    response.setContentType(
                                            "application/json"); // Response body là JSON.

                                    response.getWriter().write(
                                            """
                                                    {
                                                      "code": 403,
                                                      "message": "Forbidden",
                                                      "details": "You do not have permission"
                                                    }
                                                    """);
                                }))

                // Hãy chạy jwtFilter trước UsernamePasswordAuthenticationFilter.
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build(); // Tạo ra SecurityFilterChain hoàn chỉnh cho Spring sử dụng.
    }
}