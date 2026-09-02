package com.redcheck.backend.exception;

public class SubjectNotOwnedException extends RuntimeException {

    public SubjectNotOwnedException() {
        super("You don't have permission to modify this subject");
    }
}