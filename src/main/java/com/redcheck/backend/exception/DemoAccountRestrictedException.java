package com.redcheck.backend.exception;

public class DemoAccountRestrictedException extends RuntimeException {

    public DemoAccountRestrictedException() {
        super("This action is not available for the demo account");
    }
}
