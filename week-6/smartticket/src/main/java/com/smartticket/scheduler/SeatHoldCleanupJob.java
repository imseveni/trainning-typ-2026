package com.smartticket.scheduler;

import com.smartticket.enums.HoldStatus;
import com.smartticket.repository.SeatHoldRepository;
import com.smartticket.service.SeatHoldService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SeatHoldCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldCleanupJob.class);

    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldService seatHoldService;

    // Chạy sau mỗi 30 giây kể từ khi lần trước hoàn tất
    @Scheduled(fixedDelay = 30000)
    public void cleanupExpiredHolds() {

        List<Long> expiredIds = seatHoldRepository
                .findExpiredHoldIds(
                        HoldStatus.ACTIVE,
                        Instant.now(),
                        PageRequest.of(0, 100));

        for (Long seatHoldId : expiredIds) {

            try {
                seatHoldService.expireSeatHold(seatHoldId);
            } catch (RuntimeException ex) {
                log.warn(
                        "Cannot expire seat hold {}",
                        seatHoldId,
                        ex);
            }
        }
    }
}
