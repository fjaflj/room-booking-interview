package com.fwdrobo.roombooking.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingWindowPolicyTest {

    private final BookingWindowPolicy policy = new BookingWindowPolicy();

    private final LocalDateTime start = LocalDateTime.parse("2030-01-15T09:00:00");

    @Test
    void returnsMissingBoundaryWhenEitherEndpointIsAbsent() {
        assertEquals(BookingWindowResult.MISSING_BOUNDARY, policy.evaluate(null, start));
        assertEquals(BookingWindowResult.MISSING_BOUNDARY, policy.evaluate(start, null));
    }

    @Test
    void checksEndOrderingBeforeDuration() {
        assertEquals(BookingWindowResult.END_NOT_AFTER_START, policy.evaluate(start, start));
        assertEquals(
                BookingWindowResult.END_NOT_AFTER_START,
                policy.evaluate(start, start.minusMinutes(1)));
    }

    @Test
    void acceptsInclusiveMinimumAndMaximumDurations() {
        assertEquals(BookingWindowResult.VALID, policy.evaluate(start, start.plusMinutes(30)));
        assertEquals(BookingWindowResult.VALID, policy.evaluate(start, start.plusMinutes(120)));
    }

    @Test
    void rejectsDurationsImmediatelyOutsideTheAllowedRange() {
        assertEquals(
                BookingWindowResult.DURATION_OUT_OF_RANGE,
                policy.evaluate(start, start.plusMinutes(29)));
        assertEquals(
                BookingWindowResult.DURATION_OUT_OF_RANGE,
                policy.evaluate(start, start.plusMinutes(121)));
    }
}
