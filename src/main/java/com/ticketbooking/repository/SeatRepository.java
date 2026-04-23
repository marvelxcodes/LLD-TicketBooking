package com.ticketbooking.repository;

import com.ticketbooking.model.Seat;
import com.ticketbooking.enums.SeatCategory;
import com.ticketbooking.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Seat entity operations.
 * Provides CRUD operations and custom query methods for seat management.
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Finds all seats for a specific event.
     */
    List<Seat> findByEventId(Long eventId);

    /**
     * Finds available seats for a specific event.
     */
    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeatsByEventId(@Param("eventId") Long eventId);

    /**
     * Finds seats by status for a specific event.
     */
    List<Seat> findByEventIdAndStatus(Long eventId, SeatStatus status);

    /**
     * Finds seats by category for a specific event.
     */
    List<Seat> findByEventIdAndCategory(Long eventId, SeatCategory category);

    /**
     * Finds available seats by category for a specific event.
     */
    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.category = :category AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeatsByEventIdAndCategory(
            @Param("eventId") Long eventId, 
            @Param("category") SeatCategory category);

    /**
     * Finds a seat by seat number and event ID.
     */
    Optional<Seat> findBySeatNumberAndEventId(String seatNumber, Long eventId);

    /**
     * Counts available seats for a specific event.
     */
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.event.id = :eventId AND s.status = 'AVAILABLE'")
    long countAvailableSeatsByEventId(@Param("eventId") Long eventId);

    /**
     * Finds locked seats for a specific event.
     */
    List<Seat> findByEventIdAndStatus(Long eventId, SeatStatus LOCKED);
}
