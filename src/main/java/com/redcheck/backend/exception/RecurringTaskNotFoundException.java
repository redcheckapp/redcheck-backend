package com.redcheck.backend.exception;

public class RecurringTaskNotFoundException extends RuntimeException {

    public RecurringTaskNotFoundException(Long id) {
        super("Recurring task with id '" + id + "' not found");
    }
}