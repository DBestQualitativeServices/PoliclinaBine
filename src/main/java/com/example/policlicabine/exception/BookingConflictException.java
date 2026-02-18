package com.example.policlicabine.exception;

import com.example.policlicabine.dto.BookingConflictDto;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when an appointment booking conflicts with existing appointments.
 * Returns HTTP 409 with detailed conflict information.
 */
@Getter
public class BookingConflictException extends RuntimeException {

    private final List<BookingConflictDto> conflicts;

    public BookingConflictException(String message, List<BookingConflictDto> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    /**
     * Factory method to create exception from conflict list with auto-generated message.
     */
    public static BookingConflictException of(List<BookingConflictDto> conflicts) {
        int count = conflicts != null ? conflicts.size() : 0;
        String message = String.format("Booking conflict: Doctor has %d overlapping appointment%s",
            count, count == 1 ? "" : "s");
        return new BookingConflictException(message, conflicts);
    }
}
