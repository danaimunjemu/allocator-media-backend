package com.allocator.authservice.exception;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException() {
        super("Current password is incorrect");
    }
}
