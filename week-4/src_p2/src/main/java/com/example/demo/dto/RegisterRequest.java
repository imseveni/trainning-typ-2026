package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Thêm email vào dữ liệu đăng ký
// Cần thêm email vì consumer phải biết gửi mail đến địa chỉ nào.
public record RegisterRequest(String username, String password,
        @NotBlank @Email String email) {
}
