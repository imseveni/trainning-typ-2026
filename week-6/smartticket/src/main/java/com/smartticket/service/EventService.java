package com.smartticket.service;

import com.smartticket.dto.*;
import com.smartticket.model.Event;
import com.smartticket.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;

    // 1. Tạo sự kiện
    @Transactional
    public EventResponse createEvent(EventRequest request) {

        Event event = new Event();

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setVenue(request.venue());

        return toEventResponse(eventRepository.save(event));
    }

    // 2. Lấy danh sách sự kiện có phân trang
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getEvents(
            int page, int size) {

        if (page < 1 || size < 1 || size > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page or size is invalid");
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by("eventId").descending());

        Page<EventResponse> result = eventRepository
                .findAll(pageable)
                .map(this::toEventResponse);

        return PageResponse.from(result);
    }

    // 3. Lấy sự kiện theo ID
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {

        return toEventResponse(findEventById(eventId));
    }

    // 4. Cập nhật sự kiện
    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request) {

        Event event = findEventById(eventId);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setVenue(request.venue());

        return toEventResponse(eventRepository.save(event));
    }

    // 5. Xóa sự kiện
    @Transactional
    public void deleteEvent(Long eventId) {

        Event event = findEventById(eventId);

        if (showtimeRepository.existsByEvent_EventId(eventId)
                || seatRepository.existsByEvent_EventId(eventId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delete showtimes and seats first");
        }

        eventRepository.delete(event);
    }

    // Hàm hỗ trợ tìm Event
    private Event findEventById(Long eventId) {

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"));
    }

    // Chuyển Entity thành Response DTO
    private EventResponse toEventResponse(Event e) {

        return new EventResponse(
                e.getEventId(),
                e.getTitle(),
                e.getDescription(),
                e.getVenue());
    }
}
