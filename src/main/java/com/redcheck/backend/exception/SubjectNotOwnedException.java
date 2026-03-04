package com.redcheck.backend.exception;

public class SubjectNotOwnedException extends RuntimeException {
    public SubjectNotOwnedException(String message) {
        super(message);
    }
}
