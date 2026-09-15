package com.fwdrobo.roombooking.api;

import java.net.URI;

import com.fwdrobo.roombooking.repository.InMemoryBookingRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryBookingRepository bookingRepository;

    @Test
    void returnsExistingBooking() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-1011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("booking-1011"))
                .andExpect(jsonPath("$.roomId").value("room-101"))
                .andExpect(jsonPath("$.start").value("2030-01-15T09:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T09:30:00"));
    }

    @Test
    void returnsNotFoundForMissingRoom() throws Exception {
        mockMvc.perform(get("/rooms/room-missing/bookings/booking-1011"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/rooms/room-missing/bookings/booking-1011"));
    }

    @Test
    void returnsNotFoundForMissingBookingWithoutExposingExceptionDetails() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/rooms/room-101/bookings/booking-missing"))
                .andExpect(content().string(not(containsString("BookingNotFoundException"))))
                .andExpect(content().string(not(containsString(" at com.fwdrobo"))));
    }

    @Test
    void createsBookingAndReturnsLocationThatCanBeQueried() throws Exception {
        long countBefore = bookingRepository.countByRoomId("room-101");

        MvcResult result = mockMvc.perform(post("/rooms/room-101/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-15T10:00:00", "2030-01-15T10:30:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("room-101"))
                .andExpect(jsonPath("$.start").value("2030-01-15T10:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T10:30:00"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String bookingId = JsonPath.read(responseBody, "$.id");
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        assertTrue(bookingId.startsWith("booking-"));
        assertEquals("/rooms/room-101/bookings/" + bookingId, URI.create(location).getPath());
        assertEquals(countBefore + 1, bookingRepository.countByRoomId("room-101"));

        mockMvc.perform(get(URI.create(location).getPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.roomId").value("room-101"));
    }

    @Test
    void rejectsInvalidWindowWithoutCreatingBooking() throws Exception {
        long countBefore = bookingRepository.countByRoomId("room-101");

        mockMvc.perform(post("/rooms/room-101/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-15T10:00:00", "2030-01-15T10:15:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BOOKING_WINDOW"));

        assertEquals(countBefore, bookingRepository.countByRoomId("room-101"));
    }

    @Test
    void rejectsMissingRoomWithoutCreatingBooking() throws Exception {
        long countBefore = bookingRepository.countByRoomId("room-missing");

        mockMvc.perform(post("/rooms/room-missing/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-15T10:00:00", "2030-01-15T10:30:00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));

        assertEquals(countBefore, bookingRepository.countByRoomId("room-missing"));
    }

    @Test
    void rejectsConflictingBookingWithoutCreatingBooking() throws Exception {
        long countBefore = bookingRepository.countByRoomId("room-202");

        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-15T10:15:00", "2030-01-15T10:45:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));

        assertEquals(countBefore, bookingRepository.countByRoomId("room-202"));
    }

    @Test
    void allowsBookingsAdjacentToExistingIntervals() throws Exception {
        long countBefore = bookingRepository.countByRoomId("room-202");

        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-15T10:30:00", "2030-01-15T11:00:00")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2030-01-15T11:30:00", "2030-01-15T12:00:00")))
                .andExpect(status().isCreated());

        assertEquals(countBefore + 2, bookingRepository.countByRoomId("room-202"));
    }

    private static String requestBody(String start, String end) {
        return "{\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
    }
}
