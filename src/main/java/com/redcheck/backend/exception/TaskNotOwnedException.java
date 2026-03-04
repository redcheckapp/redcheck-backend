package com.redcheck.backend.exception;

public class TaskNotOwnedException extends RuntimeException {
    public TaskNotOwnedException(String message) {
        super(message);
    }
}
