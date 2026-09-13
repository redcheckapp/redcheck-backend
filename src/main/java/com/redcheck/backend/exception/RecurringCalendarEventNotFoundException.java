package com.redcheck.backend.exception;

public class RecurringCalendarEventNotFoundException extends RuntimeException {

    public RecurringCalendarEventNotFoundException(Long id) {
        super("Recurring calendar event with id '" + id + "' not found");
    }
}
