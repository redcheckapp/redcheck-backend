package com.redcheck.backend.exception;

public class TaskNotOwnedException extends RuntimeException {
    public TaskNotOwnedException() {
        super("You don't have permission to modify this task");
    }
}
