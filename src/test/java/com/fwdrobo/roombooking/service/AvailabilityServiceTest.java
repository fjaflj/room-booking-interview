package com.fwdrobo.roombooking.service;

import java.time.LocalDateTime;

import com.fwdrobo.roombooking.repository.InMemoryBookingRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityServiceTest {

    private final AvailabilityService availabilityService =
            new AvailabilityService(new InMemoryBookingRepository());

    @Test
    void preservesConflictWithFirstBookingWhenLaterBookingDoesNotOverlap() {
        assertFalse(availabilityService.isAvailable(
                "room-202", at("10:15"), at("10:20")));
    }

    @Test
    void returnsUnavailableWhenCandidateOverlapsLaterBooking() {
        assertFalse(availabilityService.isAvailable(
                "room-202", at("12:10"), at("12:20")));
    }

    @Test
    void returnsAvailableWhenNoBookingOverlaps() {
        assertTrue(availabilityService.isAvailable(
                "room-202", at("11:00"), at("11:30")));
    }

    @Test
    void treatsCandidateTouchingExistingEndAsAvailable() {
        assertTrue(availabilityService.isAvailable(
                "room-202", at("10:30"), at("11:00")));
    }

    @Test
    void treatsCandidateTouchingExistingStartAsAvailable() {
        assertTrue(availabilityService.isAvailable(
                "room-202", at("09:30"), at("10:00")));
    }

    private static LocalDateTime at(String time) {
        return LocalDateTime.parse("2030-01-15T" + time + ":00");
    }
}
