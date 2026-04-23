package com.ticketbooking.repository;

import com.ticketbooking.model.Event;
import com.ticketbooking.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Event entity operations.
 * Provides CRUD operations and custom query methods for event searches.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Finds all active events.
     */
    List<Event> findByIsActiveTrue();

    /**
     * Finds events by city and active status.
     */
    List<Event> findByCityAndIsActiveTrue(String city);

    /**
     * Finds events by type and active status.
     */
    List<Event> findByEventTypeAndIsActiveTrue(EventType eventType);

    /**
     * Finds events occurring on a specific date.
     */
    @Query("SELECT e FROM Event e WHERE e.eventDate = :date AND e.isActive = true")
    List<Event> findEventsByDate(@Param("date") LocalDate date);

    /**
     * Finds events by city and date.
     */
    @Query("SELECT e FROM Event e WHERE e.city = :city AND e.eventDate = :date AND e.isActive = true")
    List<Event> findEventsByCityAndDate(@Param("city") String city, @Param("date") LocalDate date);

    /**
     * Searches events by name containing the given keyword.
     */
    @Query("SELECT e FROM Event e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND e.isActive = true")
    List<Event> searchEventsByName(@Param("keyword") String keyword);

    /**
     * Finds events organized by a specific user.
     */
    List<Event> findByOrganizerIdAndIsActiveTrue(Long organizerId);

    /**
     * Finds events with available seats.
     */
    @Query("SELECT e FROM Event e WHERE e.availableSeats > 0 AND e.isActive = true")
    List<Event> findEventsWithAvailableSeats();
}
