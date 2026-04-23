package com.ticketbooking.enums;

/**
 * Represents the status of a seat in an event.
 */
public enum SeatStatus {
    /** Seat is available for booking */
    AVAILABLE,
    /** Seat is temporarily locked during booking process */
    LOCKED,
    /** Seat has been booked and confirmed */
    BOOKED
}
