package com.smartticket.repository;

import com.smartticket.model.ShowtimeSeat;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface ShowtimeSeatRepository
        extends JpaRepository<ShowtimeSeat, Long> {

    @Query("""
                SELECT ss
                FROM ShowtimeSeat ss
                JOIN FETCH ss.seat s
                WHERE ss.showtime.showtimeId = :showtimeId
                ORDER BY s.rowLabel ASC, s.seatNumber ASC
            """)
    List<ShowtimeSeat> findSeatMapByShowtimeId(
            @Param("showtimeId") Long showtimeId);

    @Modifying
    @Query("""
                DELETE FROM ShowtimeSeat ss
                WHERE ss.showtime.showtimeId = :showtimeId
            """)
    int deleteByShowtimeId(
            @Param("showtimeId") Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT ss
                FROM ShowtimeSeat ss
                WHERE ss.showtimeSeatId = :showtimeSeatId
            """)
    Optional<ShowtimeSeat> findByShowtimeSeatIdForUpdate(
            @Param("showtimeSeatId") Long showtimeSeatId);

}
