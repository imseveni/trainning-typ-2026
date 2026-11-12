package com.smartticket.service;

import com.smartticket.dto.*;
import com.smartticket.model.*;
import com.smartticket.enums.*;
import com.smartticket.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldService.class);
    private static final long HOLD_SECONDS = 600;

    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;

    // 1. Giữ ghế trong 10 phút
    @Transactional
    public SeatHoldResponse createSeatHold(
            SeatHoldRequest request, Long userId) {

        return createSeatHoldInternal(request, userId);
    }

    private SeatHoldResponse createSeatHoldInternal(
            SeatHoldRequest request, Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        Showtime showtime = showtimeRepository
                .findById(request.showtimeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Showtime not found"));

        List<Long> seatIds = request.showtimeSeatIds()
                .stream()
                .sorted()
                .toList();

        if (seatIds.stream().distinct().count() != seatIds.size()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate seat IDs");
        }

        // Khóa tất cả ghế theo thứ tự ID tăng dần
        List<ShowtimeSeat> lockedSeats = lockSeats(seatIds);

        Instant now = Instant.now();

        // Kiểm tra tất cả ghế trước khi giữ
        for (ShowtimeSeat seat : lockedSeats) {

            if (!seat.getShowtime().getShowtimeId()
                    .equals(showtime.getShowtimeId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Seat does not belong to showtime");
            }

            if (!canHold(seat, now)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat is unavailable: "
                                + seat.getShowtimeSeatId());
            }
        }

        // Tạo lượt giữ
        SeatHold hold = new SeatHold();

        hold.setUser(user);
        hold.setShowtime(showtime);
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setCreatedAt(now);
        hold.setExpiresAt(now.plusSeconds(HOLD_SECONDS));

        SeatHold savedHold = seatHoldRepository.save(hold);

        List<SeatHoldItem> items = new ArrayList<>();

        // Chuyển toàn bộ ghế sang HELD
        for (ShowtimeSeat seat : lockedSeats) {

            seat.setStatus(SeatStatus.HELD);
            seat.setActiveHold(savedHold);

            SeatHoldItem item = new SeatHoldItem();

            item.setHold(savedHold);
            item.setShowtimeSeat(seat);

            items.add(item);
        }

        seatHoldItemRepository.saveAll(items);

        return toResponse(savedHold, seatIds);
    }

    // 2. Hủy một lượt giữ ghế
    @Transactional
    public SeatHoldResponse cancelSeatHold(
            Long seatHoldId, Long userId) {

        SeatHold hold = findHold(seatHoldId);
        checkOwner(hold, userId);

        List<Long> seatIds = seatHoldItemRepository
                .findShowtimeSeatIdsByHoldId(seatHoldId);

        List<ShowtimeSeat> lockedSeats = lockSeats(seatIds);

        // Khóa và đọc lại trạng thái mới nhất của lượt giữ
        entityManager.refresh(
                hold, LockModeType.PESSIMISTIC_WRITE);

        if (hold.getStatus() == HoldStatus.CONFIRMED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirmed booking cannot be cancelled here");
        }

        if (hold.getStatus() == HoldStatus.ACTIVE) {

            Instant now = Instant.now();

            hold.setStatus(
                    hold.getExpiresAt().isAfter(now)
                            ? HoldStatus.CANCELLED
                            : HoldStatus.EXPIRED);

            releaseOwnedSeats(lockedSeats, seatHoldId);
        }

        return toResponse(hold, seatIds);
    }

    // 3. Xử lý một lượt giữ đã hết hạn
    @Transactional
    public void expireSeatHold(Long seatHoldId) {

        SeatHold hold = findHold(seatHoldId);

        List<Long> seatIds = seatHoldItemRepository
                .findShowtimeSeatIdsByHoldId(seatHoldId);

        List<ShowtimeSeat> lockedSeats = lockSeats(seatIds);

        entityManager.refresh(
                hold,
                LockModeType.PESSIMISTIC_WRITE);

        Instant now = Instant.now();

        if (hold.getStatus() != HoldStatus.ACTIVE
                || hold.getExpiresAt().isAfter(now)) {
            return;
        }

        hold.setStatus(HoldStatus.EXPIRED);

        releaseOwnedSeats(lockedSeats, seatHoldId);
    }

    // 4. Lấy thông tin lượt giữ
    @Transactional(readOnly = true)
    public SeatHoldResponse getSeatHold(
            Long seatHoldId, Long userId) {

        SeatHold hold = findHold(seatHoldId);
        checkOwner(hold, userId);

        List<Long> seatIds = seatHoldItemRepository
                .findShowtimeSeatIdsByHoldId(seatHoldId);

        return toResponse(hold, seatIds);
    }

    // 5. Khóa từng ghế theo thứ tự tăng dần
    private List<ShowtimeSeat> lockSeats(
            List<Long> seatIds) {

        List<ShowtimeSeat> result = new ArrayList<>();

        for (Long seatId : seatIds) {

            ShowtimeSeat seat = showtimeSeatRepository
                    .findByShowtimeSeatIdForUpdate(seatId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "ShowtimeSeat not found: " + seatId));

            result.add(seat);
        }

        return result;
    }

    // 6. Kiểm tra một ghế có thể giữ không
    private boolean canHold(
            ShowtimeSeat seat, Instant now) {

        if (seat.getStatus() == SeatStatus.AVAILABLE) {
            return seat.getActiveHold() == null;
        }

        if (seat.getStatus() == SeatStatus.SOLD) {
            return false;
        }

        if (seat.getStatus() == SeatStatus.HELD) {

            SeatHold holdReference = seat.getActiveHold();
            if (holdReference == null) {
                return false;
            }

            // ShowtimeSeat được đọc bằng SELECT ... FOR UPDATE sau khi có thể
            // đã phải chờ request khác. Lazy load activeHold bằng SELECT thường
            // có thể dùng snapshot cũ của transaction (MySQL REPEATABLE READ),
            // không nhìn thấy hold vừa được request thắng tạo và commit.
            // Đọc hold bằng locking read để lấy trạng thái mới nhất trong DB.
            Long oldHoldId = holdReference.getSeatHoldId();
            Optional<SeatHold> currentHold = seatHoldRepository
                    .findByIdForUpdate(oldHoldId);

            if (currentHold.isEmpty()) {
                // Chỉ xảy ra nếu dữ liệu cũ có active_hold_id mồ côi.
                // Ghế đã bị khóa nên có thể sửa liên kết an toàn tại đây.
                log.warn("Seat {} references missing hold {}; releasing stale link",
                        seat.getShowtimeSeatId(), oldHoldId);
                seat.setActiveHold(null);
                seat.setStatus(SeatStatus.AVAILABLE);
                return true;
            }

            SeatHold oldHold = currentHold.get();
            seat.setActiveHold(oldHold);

            return oldHold != null
                    && oldHold.getStatus() == HoldStatus.ACTIVE
                    && !oldHold.getExpiresAt().isAfter(now);
        }

        return false;
    }

    // 7. Chỉ giải phóng ghế còn thuộc lượt giữ này
    private void releaseOwnedSeats(
            List<ShowtimeSeat> seats, Long seatHoldId) {

        for (ShowtimeSeat seat : seats) {

            SeatHold activeHold = seat.getActiveHold();

            if (seat.getStatus() == SeatStatus.HELD
                    && activeHold != null
                    && activeHold.getSeatHoldId()
                            .equals(seatHoldId)) {

                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setActiveHold(null);
            }
        }
    }

    // 8. Tìm lượt giữ theo khóa chính
    private SeatHold findHold(Long seatHoldId) {

        return seatHoldRepository.findById(seatHoldId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SeatHold not found"));
    }

    // 9. Kiểm tra quyền sở hữu
    private void checkOwner(
            SeatHold hold, Long userId) {

        if (!hold.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This seat hold belongs to another user");
        }
    }

    // 10. Chuyển Entity thành DTO
    private SeatHoldResponse toResponse(
            SeatHold hold, List<Long> seatIds) {

        return new SeatHoldResponse(
                hold.getSeatHoldId(),
                hold.getUser().getUserId(),
                hold.getShowtime().getShowtimeId(),
                hold.getStatus(),
                hold.getExpiresAt(),
                seatIds);
    }

    // Xác nhận lượt giữ và tạo Booking
    @Transactional
    public BookingResponse confirmSeatHold(
            Long seatHoldId, Long userId) {

        // 1. Tìm lượt giữ và kiểm tra chủ sở hữu
        SeatHold hold = findHold(seatHoldId);

        checkOwner(hold, userId);

        // 2. Lấy tất cả ID ghế thuộc lượt giữ
        List<Long> showtimeSeatIds = seatHoldItemRepository
                .findShowtimeSeatIdsByHoldId(seatHoldId)
                .stream()
                .sorted()
                .toList();

        if (showtimeSeatIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat hold has no seats");
        }

        // 3. Khóa tất cả ghế theo thứ tự tăng dần
        List<ShowtimeSeat> lockedSeats = lockSeats(showtimeSeatIds);

        // 4. Khóa và đọc lại SeatHold mới nhất
        entityManager.refresh(
                hold,
                LockModeType.PESSIMISTIC_WRITE);

        checkOwner(hold, userId);

        // 5. Nếu đã xác nhận trước đó,
        // trả lại Booking cũ, không tạo Booking mới
        if (hold.getStatus() == HoldStatus.CONFIRMED) {

            Booking existingBooking = bookingRepository
                    .findBySeatHoldIdForUpdate(seatHoldId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Confirmed hold has no booking"));

            return toBookingResponse(
                    existingBooking, showtimeSeatIds);
        }

        // 6. Chỉ ACTIVE mới được xác nhận
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat hold is not active");
        }

        // 7. Kiểm tra thời hạn sau khi đã lấy khóa
        Instant now = Instant.now();

        if (!hold.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat hold has expired");
        }

        // 8. Kiểm tra ghế vẫn thuộc lượt giữ hiện tại
        for (ShowtimeSeat seat : lockedSeats) {

            SeatHold activeHold = seat.getActiveHold();

            boolean belongsToHold = activeHold != null
                    && activeHold.getSeatHoldId()
                            .equals(seatHoldId);

            boolean sameShowtime = seat.getShowtime().getShowtimeId()
                    .equals(hold.getShowtime().getShowtimeId());

            if (seat.getStatus() != SeatStatus.HELD
                    || !belongsToHold
                    || !sameShowtime) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat is no longer reserved: "
                                + seat.getShowtimeSeatId());
            }
        }

        // 9. Tạo Booking
        Booking booking = new Booking();

        booking.setHold(hold);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(now);

        Booking savedBooking = bookingRepository.save(booking);


        // 10. Xác nhận SeatHold
        hold.setStatus(HoldStatus.CONFIRMED);

        // 11. Chuyển tất cả ghế HELD thành SOLD
        for (ShowtimeSeat seat : lockedSeats) {
            seat.setStatus(SeatStatus.SOLD);
            seat.setActiveHold(null);
        }

        // 12. Trả về BookingResponse
        return toBookingResponse(
                savedBooking, showtimeSeatIds);
    }

    // Chuyển Booking Entity thành DTO
    private BookingResponse toBookingResponse(
            Booking booking,
            List<Long> showtimeSeatIds) {

        SeatHold hold = booking.getHold();

        return new BookingResponse(
                booking.getBookingId(),
                hold.getSeatHoldId(),
                hold.getUser().getUserId(),
                hold.getShowtime().getShowtimeId(),
                booking.getStatus(),
                booking.getCreatedAt(),
                showtimeSeatIds);
    }

}
