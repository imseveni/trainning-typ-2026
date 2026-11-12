package com.smartticket.repository;

import com.smartticket.model.Booking;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT b
                FROM Booking b
                WHERE b.hold.seatHoldId = :seatHoldId
            """)
    Optional<Booking> findBySeatHoldIdForUpdate(
            @Param("seatHoldId") Long seatHoldId);
}
