package com.smartticket.repository;

import com.smartticket.model.SeatHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatHoldItemRepository
        extends JpaRepository<SeatHoldItem, Long> {

    @Query("""
                SELECT i.showtimeSeat.showtimeSeatId
                FROM SeatHoldItem i
                WHERE i.hold.seatHoldId = :seatHoldId
                ORDER BY i.showtimeSeat.showtimeSeatId ASC
            """)
    List<Long> findShowtimeSeatIdsByHoldId(
            @Param("seatHoldId") Long seatHoldId);
}
