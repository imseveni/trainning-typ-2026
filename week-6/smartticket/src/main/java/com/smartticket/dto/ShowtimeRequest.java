package com.smartticket.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.math.BigDecimal;

public record ShowtimeRequest(
        @NotNull @Future Instant startAt,
        @NotNull @DecimalMin("0.0") BigDecimal ticketPrice) {
}
