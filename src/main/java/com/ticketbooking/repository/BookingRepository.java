package com.ticketbooking.repository;

import com.ticketbooking.model.Booking;
import com.ticketbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Booking entity operations.
 * Provides CRUD operations and custom query methods for booking management.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds a booking by its reference number.
     */
    Optional<Booking> findByBookingReference(String bookingReference);

    /**
     * Finds all bookings for a specific user.
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Finds bookings by user and status.
     */
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Finds all bookings with a specific status.
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Finds pending bookings that have expired.
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.expiryTime < :currentTime")
    List<Booking> findExpiredBookings(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Finds bookings for a specific event.
     */
    List<Booking> findByEventId(Long eventId);

    /**
     * Finds confirmed bookings for a specific event.
     */
    @Query("SELECT b FROM Booking b WHERE b.event.id = :eventId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByEventId(@Param("eventId") Long eventId);

    /**
     * Checks if a booking reference exists.
     */
    boolean existsByBookingReference(String bookingReference);
}
