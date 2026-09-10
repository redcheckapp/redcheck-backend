package com.redcheck.backend.exception;

public class NewPasswordSameAsCurrentException extends RuntimeException {

    public NewPasswordSameAsCurrentException() {
        super("The new password must be different from the current one");
    }
}
