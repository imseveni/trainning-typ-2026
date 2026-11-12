package com.smartticket.dto;

import jakarta.validation.constraints.NotBlank;

public record EventRequest(
        @NotBlank String title,
        String description,
        @NotBlank String venue) {
}
