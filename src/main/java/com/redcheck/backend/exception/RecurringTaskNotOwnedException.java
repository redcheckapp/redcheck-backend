package com.redcheck.backend.exception;

public class RecurringTaskNotOwnedException extends RuntimeException {
    public RecurringTaskNotOwnedException() {
        super("You don't have permission to modify this recurring task");
    }
}
