package com.fwdrobo.roombooking.api;

import java.time.LocalDateTime;

public record BookingRequest(LocalDateTime start, LocalDateTime end) {
}
