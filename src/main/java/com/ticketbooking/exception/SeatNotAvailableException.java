package com.ticketbooking.exception;

/**
 * Exception thrown when a seat is not available for booking.
 */
public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException(String message) {
        super(message);
    }

    public SeatNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
