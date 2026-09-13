package com.redcheck.backend.exception;

public class EventCategoryNotOwnedException extends RuntimeException {

    public EventCategoryNotOwnedException() {
        super("You don't have permission to modify this event category");
    }
}
