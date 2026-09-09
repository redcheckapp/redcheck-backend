package com.redcheck.backend.exception;

public class InvalidGoogleTokenException extends RuntimeException {

    public InvalidGoogleTokenException() {
        super("Invalid Google ID token");
    }
}
