package com.redcheck.backend.exception;

public class CalendarEventNotFoundException extends RuntimeException {

    public CalendarEventNotFoundException(Long id) {
        super("Calendar event with id '" + id + "' not found");
    }
}
