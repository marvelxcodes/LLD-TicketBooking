package com.ticketbooking.exception;

/**
 * Exception thrown when a booking operation is invalid.
 */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
