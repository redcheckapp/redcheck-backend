package com.redcheck.backend.exception;

public class SubjectAlreadyExistsException extends RuntimeException {

    public SubjectAlreadyExistsException(String name) {
        super("Subject '" + name + "' already exists");
    }
}