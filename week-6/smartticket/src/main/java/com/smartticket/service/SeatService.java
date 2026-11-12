
package com.smartticket.service;

import com.smartticket.dto.*;
import com.smartticket.model.*;
import com.smartticket.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final EventRepository eventRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    // 1. Tạo sơ đồ ghế cho Event
    @Transactional
    public SeatGenerationResponse generateSeatsForEvent(
            Long eventId, SeatGenerateRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"));

        if (seatRepository.existsByEvent_EventId(eventId)
                || showtimeRepository.existsByEvent_EventId(eventId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat layout already exists or showtimes exist");
        }

        List<Seat> seats = new ArrayList<>();

        for (int r = 0; r < request.rows(); r++) {

            String rowLabel = String.valueOf((char) ('A' + r));

            for (int number = 1; number <= request.seatsPerRow(); number++) {

                Seat seat = new Seat();

                seat.setEvent(event);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(number);

                seats.add(seat);
            }
        }

        seatRepository.saveAll(seats);

        return new SeatGenerationResponse(
                eventId,
                seats.size());
    }

    // 2. Lấy sơ đồ ghế của Showtime
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMapByShowtimeId(Long showtimeId) {

        if (!showtimeRepository.existsById(showtimeId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Showtime not found");
        }

        return showtimeSeatRepository.findSeatMapByShowtimeId(showtimeId)
                .stream()
                .map(showtimeSeat -> new SeatResponse(
                        showtimeSeat.getShowtimeSeatId(),
                        showtimeSeat.getSeat().getSeatId(),
                        showtimeSeat.getSeat().getRowLabel(),
                        showtimeSeat.getSeat().getSeatNumber(),
                        showtimeSeat.getStatus()))
                .toList();
    }

    // 3. Xóa sơ đồ ghế của Event
    @Transactional
    public void deleteSeatsByEventId(Long eventId) {

        if (!eventRepository.existsById(eventId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event not found");
        }

        if (showtimeRepository.existsByEvent_EventId(eventId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delete showtimes before deleting seats");
        }

        List<Seat> seats = seatRepository.findByEvent_EventIdOrderByRowLabelAscSeatNumberAsc(
                eventId);

        seatRepository.deleteAll(seats);
    }
}
