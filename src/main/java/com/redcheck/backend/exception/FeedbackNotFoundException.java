package com.redcheck.backend.exception;

public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(Long id) {
        super("Feedback with id '" + id + "' not found");
    }
}
