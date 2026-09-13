package com.redcheck.backend.exception;

public class RecurringCalendarEventNotOwnedException extends RuntimeException {

    public RecurringCalendarEventNotOwnedException() {
        super("You don't have permission to modify this recurring calendar event");
    }
}
