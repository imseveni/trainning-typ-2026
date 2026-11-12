package com.smartticket.dto;

import com.smartticket.enums.BookingStatus;

import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        Long seatHoldId,
        Long userId,
        Long showtimeId,
        BookingStatus status,
        Instant createdAt,
        List<Long> showtimeSeatIds) {
}
