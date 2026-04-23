package com.ticketbooking.exception;

/**
 * Exception thrown when a user attempts an unauthorized operation.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
