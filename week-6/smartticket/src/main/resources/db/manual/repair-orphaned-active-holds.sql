SELECT ss.showtime_seat_id, ss.status, ss.active_hold_id
FROM showtime_seats ss
LEFT JOIN seat_holds h ON h.seat_hold_id = ss.active_hold_id
WHERE ss.active_hold_id IS NOT NULL
  AND ss.showtime_seat_id > 0
  AND h.seat_hold_id IS NULL;

UPDATE showtime_seats ss
LEFT JOIN seat_holds h ON h.seat_hold_id = ss.active_hold_id
SET ss.active_hold_id = NULL,
    ss.status = CASE
        WHEN ss.status = 'HELD' THEN 'AVAILABLE'
        ELSE ss.status
    END
WHERE ss.active_hold_id IS NOT NULL
  AND ss.showtime_seat_id > 0
  AND h.seat_hold_id IS NULL;
