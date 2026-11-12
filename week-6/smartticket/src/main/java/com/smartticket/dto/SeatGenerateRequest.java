package com.smartticket.dto;

import jakarta.validation.constraints.*;

public record SeatGenerateRequest(
        @Min(1) @Max(26) int rows,
        @Min(1) @Max(50) int seatsPerRow) {
}
