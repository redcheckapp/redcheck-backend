package com.redcheck.backend.exception;

public class FeedbackAuthorDeletedException extends RuntimeException {

    public FeedbackAuthorDeletedException(Long id) {
        super("The author of feedback '" + id + "' no longer has an account");
    }
}
