package com.smartticket.controller;

import com.smartticket.dto.*;
import com.smartticket.service.ShowtimeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    // 1. Tạo suất diễn thuộc một Event
    @PostMapping("/events/{eventId}/showtimes")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowtimeResponse create(
            @PathVariable Long eventId,
            @Valid @RequestBody ShowtimeRequest request) {

        return showtimeService.createShowtime(eventId, request);
    }

    // 2. Xem các suất diễn của một Event
    @GetMapping("/events/{eventId}/showtimes")
    public List<ShowtimeResponse> getByEvent(
            @PathVariable Long eventId) {

        return showtimeService.getShowTimesByEventId(eventId);
    }

    // 3. Xem chi tiết một suất diễn
    @GetMapping("/showtimes/{showtimeId}")
    public ShowtimeResponse getById(@PathVariable Long showtimeId) {

        return showtimeService.getShowtimeById(showtimeId);
    }

    // 4. Cập nhật suất diễn
    @PutMapping("/showtimes/{showtimeId}")
    public ShowtimeResponse update(
            @PathVariable Long showtimeId,
            @Valid @RequestBody ShowtimeRequest request) {

        return showtimeService.updateShowtime(showtimeId, request);
    }

    // 5. Xóa suất diễn
    @DeleteMapping("/showtimes/{showtimeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long showtimeId) {

        showtimeService.deleteShowtime(showtimeId);
    }
}
