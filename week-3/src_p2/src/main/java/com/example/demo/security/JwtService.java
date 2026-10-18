package com.example.demo.security;

import com.example.demo.model.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    // Method này biến chuỗi secret thành một SecretKey mà thư viện JWT có thể sử
    // dụng.
    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Method tạo JWT từ AppUser
    public String generateAccessToken(AppUser user) {

        Instant now = Instant.now();

        // Bắt đầu xây dựng JWT - builder() giúp chúng ta thêm dữ liệu vào token.
        return Jwts.builder()
                .subject(user.getUsername()) // Dùng để xác định token thuộc về ai.
                .claim( // Đây là custom claim "role" tự thêm - Dùng để xác định role.
                        "role",
                        user.getRole().name())
                .claim( // Đây là custom claim "type": access tự thêm
                        // Dùng để phân biệt loại token (ở đây access token dùng để truy cập API).
                        "type",
                        "access")
                .issuedAt( // JWT được tạo vào lúc nào?
                        Date.from(now))
                .expiration( // JWT hết hạn vào lúc nào?
                        Date.from(now.plusMillis(accessExpirationMs)))
                .signWith(getSigningKey()) // Dùng để tạo Signature bằng secret key của server.
                .compact(); // Biến đổi thành chuỗi JWT: xxxxx.yyyyy.zzzzz
    }

    // Method kiểm tra token có hợp lệ hay không, rồi lấy dữ liệu bên trong token ra
    // dưới dạng Claims.
    public Claims parseAccessToken(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey()) // Dùng secret key của server để kiểm tra Signature.
                .build() // Hoàn tất việc tạo parser.
                .parseSignedClaims(token) // Đưa JWT client gửi vào để parser xử lý.
                .getPayload(); // Sau khi token hợp lệ, lấy Payload ra dưới dạng Claims.

        String type = claims.get(
                "type",
                String.class);

        if (!"access".equals(type)) {
            throw new JwtException("Invalid token type");
        }

        return claims;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }
}