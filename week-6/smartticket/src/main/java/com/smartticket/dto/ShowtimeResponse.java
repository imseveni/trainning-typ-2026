package com.smartticket.dto;

import java.time.Instant;
import java.math.BigDecimal;

public record ShowtimeResponse(
        Long showtimeId,
        Long eventId,
        Instant startAt,
        BigDecimal ticketPrice) {
}
