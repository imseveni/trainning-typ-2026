package com.smartticket.repository;

import com.smartticket.enums.HoldStatus;
import com.smartticket.model.SeatHold;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository
        extends JpaRepository<SeatHold, Long> {

    // Locking read buộc MySQL đọc phiên bản hiện tại của hold,
    // thay vì snapshot cũ đã được tạo trước khi request chờ khóa ghế.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM SeatHold h WHERE h.seatHoldId = :seatHoldId")
    Optional<SeatHold> findByIdForUpdate(
            @Param("seatHoldId") Long seatHoldId);

    boolean existsByShowtime_ShowtimeId(Long showtimeId);

    @Query("""
                SELECT h.seatHoldId
                FROM SeatHold h
                WHERE h.status = :status
                  AND h.expiresAt <= :now
                ORDER BY h.expiresAt ASC
            """)
    List<Long> findExpiredHoldIds(
            @Param("status") HoldStatus status,
            @Param("now") Instant now,
            Pageable pageable);

}
