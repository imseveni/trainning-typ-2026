package com.smartticket.model;

import com.smartticket.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "showtime_seats",
        // Một ghế chỉ có một row tồn kho trong mỗi suất diễn.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_showtime_seat",
                columnNames = { "showtime_id", "seat_id" }),
        // Hỗ trợ truy vấn ghế theo suất diễn và trạng thái.
        indexes = @Index(
                name = "idx_showtime_seat_status",
                columnList = "showtime_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class ShowtimeSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long showtimeSeatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_hold_id")
    private SeatHold activeHold;
}
