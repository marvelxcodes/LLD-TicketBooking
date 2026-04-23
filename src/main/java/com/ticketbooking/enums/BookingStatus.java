package com.ticketbooking.enums;

/**
 * Represents the status of a booking in the ticket booking system.
 */
public enum BookingStatus {
    /** Booking is being processed */
    PENDING,
    /** Booking payment is completed and confirmed */
    CONFIRMED,
    /** Booking was cancelled by user */
    CANCELLED,
    /** Booking payment failed or expired */
    EXPIRED
}
