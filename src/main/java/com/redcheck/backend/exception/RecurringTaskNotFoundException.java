package com.redcheck.backend.exception;

public class RecurringTaskNotFoundException extends RuntimeException {
    public RecurringTaskNotFoundException(Long id) {
        super("Task with id '" + id + "' not found");
    }
}
