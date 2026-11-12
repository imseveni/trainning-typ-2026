package com.smartticket.repository;

import com.smartticket.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShowtimeRepository
        extends JpaRepository<Showtime, Long> {

    boolean existsByEvent_EventId(Long eventId);

    List<Showtime> findByEvent_EventIdOrderByStartAtAsc(
            Long eventId);

}
