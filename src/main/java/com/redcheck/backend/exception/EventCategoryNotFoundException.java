package com.redcheck.backend.exception;

public class EventCategoryNotFoundException extends RuntimeException {

    public EventCategoryNotFoundException(Long id) {
        super("Event category with id '" + id + "' not found");
    }
}
