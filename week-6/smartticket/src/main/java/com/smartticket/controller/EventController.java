package com.smartticket.controller;

import com.smartticket.dto.*;
import com.smartticket.service.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 1. Tạo sự kiện
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(
            @Valid @RequestBody EventRequest request) {

        return eventService.createEvent(request);
    }

    // 2. Lấy danh sách sự kiện có phân trang
    @GetMapping
    public PageResponse<EventResponse> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        return eventService.getEvents(page, size);
    }

    // 3. Lấy thông tin một sự kiện
    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable Long id) {

        return eventService.getEventById(id);
    }

    // 4. Cập nhật sự kiện
    @PutMapping("/{id}")
    public EventResponse update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {

        return eventService.updateEvent(id, request);
    }

    // 5. Xóa sự kiện
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {

        eventService.deleteEvent(id);
    }
}
