package com.redcheck.backend.exception;

public class SubjectNotFoundException extends RuntimeException {
  public SubjectNotFoundException(String message) {
    super(message);
  }
}
