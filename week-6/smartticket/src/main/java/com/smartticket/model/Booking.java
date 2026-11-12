package com.smartticket.model;

import com.smartticket.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    // Một hold chỉ được tạo tối đa một booking; unique được áp dụng tại DB.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_hold_id", nullable = false, unique = true)
    private SeatHold hold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
