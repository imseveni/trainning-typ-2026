package com.smartticket.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record SeatHoldRequest(
        @NotNull Long showtimeId,

        @NotEmpty @Size(max = 4) List<@NotNull Long> showtimeSeatIds

) {
}
