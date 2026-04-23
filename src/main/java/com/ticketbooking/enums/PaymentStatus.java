package com.ticketbooking.enums;

/**
 * Represents the status of a payment transaction.
 */
public enum PaymentStatus {
    /** Payment is being processed */
    PENDING,
    /** Payment was successful */
    SUCCESS,
    /** Payment failed */
    FAILED,
    /** Payment was refunded */
    REFUNDED
}
