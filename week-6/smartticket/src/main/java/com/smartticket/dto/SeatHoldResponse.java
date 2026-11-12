package com.smartticket.dto;

import com.smartticket.enums.HoldStatus;
import java.time.Instant;
import java.util.List;

public record SeatHoldResponse(
        Long seatHoldId,
        Long userId,
        Long showtimeId,
        HoldStatus status,
        Instant expiresAt,
        List<Long> showtimeSeatIds) {
}
