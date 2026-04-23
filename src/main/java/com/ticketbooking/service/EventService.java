package com.ticketbooking.service;

import com.ticketbooking.dto.EventRequest;
import com.ticketbooking.dto.EventResponse;
import com.ticketbooking.dto.SeatResponse;
import com.ticketbooking.enums.SeatCategory;
import com.ticketbooking.enums.SeatStatus;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Event;
import com.ticketbooking.model.Seat;
import com.ticketbooking.model.User;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.SeatRepository;
import com.ticketbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Event management operations.
 * Handles CRUD operations, seat generation, and event searches.
 * Implements caching for frequently accessed event data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new event and generates seats based on totalSeats count.
     * Uses default pricing strategy based on seat categories distribution.
     * 
     * @param eventRequest the event creation request
     * @param organizerId the ID of the event organizer
     * @return the created EventResponse
     */
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse createEvent(EventRequest eventRequest, Long organizerId) {
        log.info("Creating event: {} for organizer: {}", eventRequest.getName(), organizerId);

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", organizerId));

        Event event = Event.builder()
                .name(eventRequest.getName())
                .description(eventRequest.getDescription())
                .venue(eventRequest.getVenue())
                .city(eventRequest.getCity())
                .eventType(eventRequest.getEventType())
                .eventDate(eventRequest.getEventDate())
                .eventTime(eventRequest.getEventTime())
                .totalSeats(eventRequest.getTotalSeats())
                .availableSeats(eventRequest.getTotalSeats())
                .organizer(organizer)
                .active(true)
                .build();

        event = eventRepository.save(event);
        log.info("Event created with ID: {}", event.getId());

        // Generate seats for the event
        generateSeatsForEvent(event, eventRequest.getTotalSeats());

        return mapToEventResponse(event);
    }

    /**
     * Generates seats for an event with distributed categories and pricing.
     * Distribution: 10% VIP, 20% Premium, 50% Standard, 20% Economy
     */
    private void generateSeatsForEvent(Event event, int totalSeats) {
        List<Seat> seats = new ArrayList<>();
        
        int vipCount = (int) (totalSeats * 0.10);
        int premiumCount = (int) (totalSeats * 0.20);
        int economyCount = (int) (totalSeats * 0.20);
        int standardCount = totalSeats - vipCount - premiumCount - economyCount;

        int seatCounter = 1;
        int rowNumber = 1;
        char rowLetter = 'A';

        // Generate VIP seats
        seats.addAll(generateSeatsForCategory(event, SeatCategory.VIP, vipCount, 
                new BigDecimal("500.00"), rowLetter, seatCounter));
        seatCounter += vipCount;
        rowLetter++;

        // Generate Premium seats
        seats.addAll(generateSeatsForCategory(event, SeatCategory.PREMIUM, premiumCount,
                new BigDecimal("300.00"), rowLetter, seatCounter));
        seatCounter += premiumCount;
        rowLetter++;

        // Generate Standard seats
        seats.addAll(generateSeatsForCategory(event, SeatCategory.STANDARD, standardCount,
                new BigDecimal("150.00"), rowLetter, seatCounter));
        seatCounter += standardCount;
        rowLetter++;

        // Generate Economy seats
        seats.addAll(generateSeatsForCategory(event, SeatCategory.ECONOMY, economyCount,
                new BigDecimal("75.00"), rowLetter, seatCounter));

        seatRepository.saveAll(seats);
        log.info("Generated {} seats for event: {}", seats.size(), event.getId());
    }

    private List<Seat> generateSeatsForCategory(Event event, SeatCategory category, 
            int count, BigDecimal price, char rowLetter, int startNumber) {
        
        List<Seat> seats = new ArrayList<>();
        String row = String.valueOf(rowLetter);

        for (int i = 0; i < count; i++) {
            Seat seat = Seat.builder()
                    .seatNumber(String.format("%s%03d", row, startNumber + i))
                    .row(row)
                    .category(category)
                    .price(price)
                    .status(SeatStatus.AVAILABLE)
                    .event(event)
                    .build();
            seats.add(seat);
        }

        return seats;
    }

    /**
     * Retrieves an event by its ID with caching.
     */
    @Cacheable(value = "events", key = "#id")
    public EventResponse getEventById(Long id) {
        log.info("Fetching event by ID: {}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
        return mapToEventResponse(event);
    }

    /**
     * Retrieves all active events.
     */
    public List<EventResponse> getAllActiveEvents() {
        log.info("Fetching all active events");
        return eventRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Searches events by city.
     */
    public List<EventResponse> getEventsByCity(String city) {
        log.info("Fetching events by city: {}", city);
        return eventRepository.findByCityAndIsActiveTrue(city)
                .stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Searches events by date.
     */
    public List<EventResponse> getEventsByDate(LocalDate date) {
        log.info("Fetching events by date: {}", date);
        return eventRepository.findEventsByDate(date)
                .stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Searches events by keyword in name.
     */
    public List<EventResponse> searchEvents(String keyword) {
        log.info("Searching events with keyword: {}", keyword);
        return eventRepository.searchEventsByName(keyword)
                .stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves available seats for an event.
     */
    public List<SeatResponse> getAvailableSeats(Long eventId) {
        log.info("Fetching available seats for event: {}", eventId);
        
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        return seatRepository.findAvailableSeatsByEventId(eventId)
                .stream()
                .map(this::mapToSeatResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves available seats for an event filtered by category.
     */
    public List<SeatResponse> getAvailableSeatsByCategory(Long eventId, SeatCategory category) {
        log.info("Fetching available seats for event: {} with category: {}", eventId, category);
        
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        return seatRepository.findAvailableSeatsByEventIdAndCategory(eventId, category)
                .stream()
                .map(this::mapToSeatResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps Event entity to EventResponse DTO.
     */
    private EventResponse mapToEventResponse(Event event) {
        List<Seat> seats = seatRepository.findByEventId(event.getId());
        
        BigDecimal minPrice = seats.stream()
                .map(Seat::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = seats.stream()
                .map(Seat::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .city(event.getCity())
                .eventType(event.getEventType())
                .eventDate(event.getEventDate())
                .eventTime(event.getEventTime())
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .active(event.isActive())
                .organizerId(event.getOrganizer() != null ? event.getOrganizer().getId() : null)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }

    /**
     * Maps Seat entity to SeatResponse DTO.
     */
    private SeatResponse mapToSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .row(seat.getRow())
                .category(seat.getCategory())
                .price(seat.getPrice())
                .status(seat.getStatus())
                .build();
    }
}
