package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService; // Cần JwtService để kiểm tra và đọc JWT.

    public JwtAuthenticationFilter(JwtService jwtService) {

        this.jwtService = jwtService;
    }

    // Viết logic kiểm tra JWT cho mỗi request.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization"); // Lấy Authorization Header

        if (header == null || !header.startsWith("Bearer ")) { // Kiểm tra có Bearer Token hay không?

            filterChain.doFilter(request, response); // Chuyển request và response sang filter tiếp theo trong chuỗi xử
                                                     // lý.

            return;
        }

        String token = header.substring(7); // Lấy token ra khỏi Bearer

        try {

            Claims claims = jwtService.parseAccessToken(token);

            String username = claims.getSubject();

            String role = claims.get(
                    "role",
                    String.class);

            var authorities = List.of(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role));

            var authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities);

            SecurityContextHolder // Request hiện tại đã được xác thực.
                    .getContext()
                    .setAuthentication(
                            authentication);

        } catch (JwtException e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Trả về 401

            response.setContentType("application/json"); // nói response là JSON.

            response.getWriter().write(
                    """
                            {
                            "code": 401,
                            "message": "Unauthorized",
                            "details": "Invalid or expired token"
                            }
                            """);

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { // Những request nào thì không cần chạy JWT filter?

        return request
                .getServletPath()
                .startsWith("/auth/");
    }
}