package com.fwdrobo.roombooking.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class BookingWindowPolicy {

    private static final Duration MIN_DURATION = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION = Duration.ofMinutes(120);

    public BookingWindowResult evaluate(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return BookingWindowResult.MISSING_BOUNDARY;
        }

        if (!end.isAfter(start)) {
            return BookingWindowResult.END_NOT_AFTER_START;
        }

        Duration duration = Duration.between(start, end);
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            return BookingWindowResult.DURATION_OUT_OF_RANGE;
        }

        return BookingWindowResult.VALID;
    }
}
