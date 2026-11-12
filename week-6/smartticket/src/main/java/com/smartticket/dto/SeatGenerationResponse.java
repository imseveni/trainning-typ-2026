package com.smartticket.dto;

public record SeatGenerationResponse(
        Long eventId,
        int createdSeats) {
}
