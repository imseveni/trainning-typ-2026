package com.smartticket.controller;

import com.smartticket.dto.*;
import com.smartticket.service.SeatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // 1. Tạo sơ đồ ghế cho sự kiện
    @PostMapping("/events/{eventId}/seats/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public SeatGenerationResponse generate(
            @PathVariable Long eventId,
            @Valid @RequestBody SeatGenerateRequest request) {

        return seatService.generateSeatsForEvent(eventId, request);
    }

    // 2. Xem sơ đồ ghế của một suất diễn
    @GetMapping("/showtimes/{showtimeId}/seats")
    public List<SeatResponse> getSeatMap(
            @PathVariable Long showtimeId) {

        return seatService.getSeatMapByShowtimeId(showtimeId);
    }

    // 3. Xóa sơ đồ ghế của sự kiện
    @DeleteMapping("/events/{eventId}/seats")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLayout(
            @PathVariable Long eventId) {

        seatService.deleteSeatsByEventId(eventId);
    }
}
