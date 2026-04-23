package com.ticketbooking.repository;

import com.ticketbooking.model.Payment;
import com.ticketbooking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity operations.
 * Provides CRUD operations and custom query methods for payment tracking.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds a payment by its transaction ID.
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Finds payment for a specific booking.
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Finds all payments with a specific status.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Finds all successful payments.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Checks if a transaction ID exists.
     */
    boolean existsByTransactionId(String transactionId);
}
