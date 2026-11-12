package com.smartticket.dto;

import com.smartticket.enums.SeatStatus;

public record SeatResponse(
        Long showtimeSeatId,
        Long seatId,
        String rowLabel,
        Integer seatNumber,
        SeatStatus status) {
}
