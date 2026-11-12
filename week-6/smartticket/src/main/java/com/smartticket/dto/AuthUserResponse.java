package com.smartticket.dto;

import com.smartticket.enums.Role;

public record AuthUserResponse(
        Long userId,
        String fullName,
        String email,
        Role role) {
}
