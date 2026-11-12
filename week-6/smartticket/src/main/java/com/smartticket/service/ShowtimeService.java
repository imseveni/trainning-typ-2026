package com.smartticket.service;

import com.smartticket.dto.*;
import com.smartticket.model.*;
import com.smartticket.enums.SeatStatus;
import com.smartticket.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final EventRepository eventRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatHoldRepository seatHoldRepository;

    // 1. Tạo suất diễn
    @Transactional
    public ShowtimeResponse createShowtime(
            Long eventId, ShowtimeRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        List<Seat> seats = seatRepository.findByEvent_EventIdOrderByRowLabelAscSeatNumberAsc(
                eventId);

        if (seats.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Create seat layout before showtime");
        }

        Showtime showtime = new Showtime();

        showtime.setEvent(event);
        showtime.setStartAt(request.startAt());
        showtime.setTicketPrice(request.ticketPrice());

        showtime = showtimeRepository.save(showtime);

        List<ShowtimeSeat> showtimeSeats = new ArrayList<>();

        for (Seat seat : seats) {

            ShowtimeSeat showtimeSeat = new ShowtimeSeat();

            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus(SeatStatus.AVAILABLE);

            showtimeSeats.add(showtimeSeat);
        }

        showtimeSeatRepository.saveAll(showtimeSeats);

        return toShowtimeResponse(showtime);
    }

    // 2. Lấy các suất diễn thuộc một sự kiện
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowTimesByEventId(Long eventId) {

        if (!eventRepository.existsById(eventId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event not found");
        }

        return showtimeRepository
                .findByEvent_EventIdOrderByStartAtAsc(eventId)
                .stream()
                .map(this::toShowtimeResponse)
                .toList();
    }

    // 3. Lấy suất diễn theo ID
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long showtimeId) {

        return toShowtimeResponse(findShowtimeById(showtimeId));
    }

    // 4. Cập nhật suất diễn
    @Transactional
    public ShowtimeResponse updateShowtime(
            Long showtimeId, ShowtimeRequest request) {

        Showtime showtime = findShowtimeById(showtimeId);

        if (seatHoldRepository.existsByShowtime_ShowtimeId(showtimeId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Showtime has booking activity");
        }

        showtime.setStartAt(request.startAt());
        showtime.setTicketPrice(request.ticketPrice());

        return toShowtimeResponse(showtimeRepository.save(showtime));
    }

    // 5. Xóa suất diễn
    @Transactional
    public void deleteShowtime(Long showtimeId) {

        Showtime showtime = findShowtimeById(showtimeId);

        if (seatHoldRepository.existsByShowtime_ShowtimeId(showtimeId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Showtime has booking activity");
        }

        showtimeSeatRepository.deleteByShowtimeId(showtimeId);
        showtimeRepository.delete(showtime);
    }

    // Hàm hỗ trợ tìm Showtime
    private Showtime findShowtimeById(Long id) {

        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Showtime not found"));
    }

    // Chuyển Showtime Entity thành Response DTO
    private ShowtimeResponse toShowtimeResponse(Showtime s) {

        return new ShowtimeResponse(
                s.getShowtimeId(),
                s.getEvent().getEventId(),
                s.getStartAt(),
                s.getTicketPrice());
    }
}
