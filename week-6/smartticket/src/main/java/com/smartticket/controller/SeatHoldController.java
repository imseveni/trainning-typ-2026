package com.smartticket.controller;

import com.smartticket.dto.BookingResponse;
import com.smartticket.dto.SeatHoldRequest;
import com.smartticket.dto.SeatHoldResponse;
import com.smartticket.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holds")
@RequiredArgsConstructor
public class SeatHoldController {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldController.class);

    private final SeatHoldService seatHoldService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeatHoldResponse createSeatHold(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SeatHoldRequest request) {
        Long userId = userId(jwt);
        log.info("HOLD_CREATE_REACHED_CONTROLLER userId={} showtimeId={} seatIds={}",
                userId, request.showtimeId(), request.showtimeSeatIds());
        return seatHoldService.createSeatHold(request, userId);
    }

    @PostMapping("/{seatHoldId}/confirm")
    public BookingResponse confirmSeatHold(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long seatHoldId) {
        return seatHoldService.confirmSeatHold(seatHoldId, userId(jwt));
    }

    @GetMapping("/{seatHoldId}")
    public SeatHoldResponse getSeatHold(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long seatHoldId) {
        return seatHoldService.getSeatHold(seatHoldId, userId(jwt));
    }

    @DeleteMapping("/{seatHoldId}")
    public SeatHoldResponse cancelSeatHold(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long seatHoldId) {
        return seatHoldService.cancelSeatHold(seatHoldId, userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
