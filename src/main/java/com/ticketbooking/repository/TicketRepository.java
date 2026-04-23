package com.ticketbooking.repository;

import com.ticketbooking.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Ticket entity operations.
 * Provides CRUD operations and custom query methods for ticket management.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Finds a ticket by its ticket number.
     */
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Finds all tickets for a specific booking.
     */
    List<Ticket> findByBookingId(Long bookingId);

    /**
     * Finds all tickets for a specific user through their bookings.
     */
    List<Ticket> findByBookingUserId(Long userId);

    /**
     * Checks if a ticket number exists.
     */
    boolean existsByTicketNumber(String ticketNumber);
}
