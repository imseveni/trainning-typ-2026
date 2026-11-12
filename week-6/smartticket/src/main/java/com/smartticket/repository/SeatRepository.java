package com.smartticket.repository;

import com.smartticket.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository
        extends JpaRepository<Seat, Long> {

    boolean existsByEvent_EventId(Long eventId);

    List<Seat> findByEvent_EventIdOrderByRowLabelAscSeatNumberAsc(
            Long eventId);
}
