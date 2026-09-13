package com.redcheck.backend.exception;

public class CalendarEventNotOwnedException extends RuntimeException {

    public CalendarEventNotOwnedException() {
        super("You don't have permission to modify this calendar event");
    }
}
