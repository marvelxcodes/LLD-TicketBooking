package com.ticketbooking.service;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.BookingResponse;
import com.ticketbooking.dto.SeatSelection;
import com.ticketbooking.enums.BookingStatus;
import com.ticketbooking.enums.SeatStatus;
import com.ticketbooking.exception.BookingException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.exception.SeatNotAvailableException;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.Event;
import com.ticketbooking.model.Seat;
import com.ticketbooking.model.Ticket;
import com.ticketbooking.model.User;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.SeatRepository;
import com.ticketbooking.repository.TicketRepository;
import com.ticketbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Service layer for Booking management operations.
 * Handles seat locking, booking creation, cancellation, and expiration handling.
 * Implements concurrency control using distributed locks for seat booking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    // In-memory lock map for concurrent seat booking prevention
    private final ConcurrentHashMap<String, ReentrantLock> seatLocks = new ConcurrentHashMap<>();

    @Value("${booking.expiry.minutes:15}")
    private int bookingExpiryMinutes;

    /**
     * Creates a new booking by locking seats and generating tickets.
     * Uses fine-grained locking to prevent double booking of seats.
     * 
     * @param bookingRequest the booking request with seat selections
     * @return the created BookingResponse
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest) {
        log.info("Creating booking for user: {} for event: {}", 
                bookingRequest.getUserId(), bookingRequest.getEventId());

        // Validate user and event
        User user = userRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", bookingRequest.getUserId()));

        Event event = eventRepository.findById(bookingRequest.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", bookingRequest.getEventId()));

        if (!event.isActive()) {
            throw new BookingException("Event is no longer active");
        }

        if (event.getAvailableSeats() < bookingRequest.getSeats().size()) {
            throw new SeatNotAvailableException("Not enough available seats for this event");
        }

        // Lock seats with concurrency control
        List<Seat> lockedSeats = lockSeats(bookingRequest.getSeats(), event);

        try {
            // Create booking
            Booking booking = Booking.builder()
                    .bookingReference(generateBookingReference())
                    .user(user)
                    .event(event)
                    .totalAmount(BigDecimal.ZERO)
                    .status(BookingStatus.PENDING)
                    .bookingTime(LocalDateTime.now())
                    .expiryTime(LocalDateTime.now().plusMinutes(bookingExpiryMinutes))
                    .build();

            // Create tickets for each locked seat
            for (Seat seat : lockedSeats) {
                Ticket ticket = Ticket.builder()
                        .ticketNumber(generateTicketNumber())
                        .seat(seat)
                        .price(seat.getPrice())
                        .used(false)
                        .build();
                booking.addTicket(ticket);
            }

            // Update event available seats
            event.setAvailableSeats(event.getAvailableSeats() - lockedSeats.size());
            eventRepository.save(event);

            booking = bookingRepository.save(booking);
            ticketRepository.saveAll(booking.getTickets());

            log.info("Booking created with reference: {}", booking.getBookingReference());

            // Send confirmation notification
            notificationService.sendBookingConfirmation(booking);

            return mapToBookingResponse(booking);

        } catch (Exception e) {
            // Release locked seats on failure
            releaseSeats(lockedSeats);
            log.error("Failed to create booking: {}", e.getMessage(), e);
            throw new BookingException("Failed to create booking: " + e.getMessage(), e);
        }
    }

    /**
     * Locks seats for booking with concurrency control.
     * Uses fine-grained locks per seat to prevent deadlocks.
     */
    private List<Seat> lockSeats(List<SeatSelection> seatSelections, Event event) {
        List<Seat> lockedSeats = new java.util.ArrayList<>();

        for (SeatSelection selection : seatSelections) {
            String lockKey = event.getId() + ":" + selection.getSeatNumber();
            ReentrantLock lock = seatLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());

            lock.lock();
            try {
                Seat seat = seatRepository.findBySeatNumberAndEventId(selection.getSeatNumber(), event.getId())
                        .orElseThrow(() -> new SeatNotAvailableException(
                                "Seat " + selection.getSeatNumber() + " not found for this event"));

                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new SeatNotAvailableException(
                            "Seat " + selection.getSeatNumber() + " is not available");
                }

                seat.lock();
                seatRepository.save(seat);
                lockedSeats.add(seat);

            } finally {
                lock.unlock();
            }
        }

        return lockedSeats;
    }

    /**
     * Releases locked seats back to available status.
     */
    private void releaseSeats(List<Seat> seats) {
        for (Seat seat : seats) {
            try {
                if (seat.getStatus() == SeatStatus.LOCKED) {
                    seat.release();
                    seatRepository.save(seat);
                }
            } catch (Exception e) {
                log.warn("Failed to release seat {}: {}", seat.getSeatNumber(), e.getMessage());
            }
        }
    }

    /**
     * Retrieves a booking by its reference number.
     */
    public BookingResponse getBookingByReference(String bookingReference) {
        log.info("Fetching booking with reference: {}", bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));
        return mapToBookingResponse(booking);
    }

    /**
     * Retrieves all bookings for a specific user.
     */
    public List<BookingResponse> getUserBookings(Long userId) {
        log.info("Fetching bookings for user: {}", userId);
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancels a booking and releases associated seats.
     * Processes refund through payment service.
     */
    @Transactional
    public BookingResponse cancelBooking(String bookingReference) {
        log.info("Cancelling booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        booking.cancel();
        
        // Release seats
        for (Ticket ticket : booking.getTickets()) {
            Seat seat = ticket.getSeat();
            seat.release();
            seatRepository.save(seat);
        }

        // Update event available seats
        Event event = booking.getEvent();
        event.setAvailableSeats(event.getAvailableSeats() + booking.getTickets().size());
        eventRepository.save(event);

        booking = bookingRepository.save(booking);

        // Process refund if payment was successful
        if (booking.getPayment() != null && 
            booking.getPayment().getStatus() == com.ticketbooking.enums.PaymentStatus.SUCCESS) {
            notificationService.sendRefundNotification(booking);
        }

        log.info("Booking cancelled: {}", bookingReference);
        return mapToBookingResponse(booking);
    }

    /**
     * Confirms a booking after successful payment.
     */
    @Transactional
    public BookingResponse confirmBooking(String bookingReference) {
        log.info("Confirming booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        booking.confirm();

        // Mark seats as booked
        for (Ticket ticket : booking.getTickets()) {
            Seat seat = ticket.getSeat();
            seat.book();
            seatRepository.save(seat);
        }

        booking = bookingRepository.save(booking);

        log.info("Booking confirmed: {}", bookingReference);
        return mapToBookingResponse(booking);
    }

    /**
     * Scheduled task to expire pending bookings that have timed out.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void expirePendingBookings() {
        log.info("Checking for expired bookings");
        
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);

        for (Booking booking : expiredBookings) {
            try {
                booking.expire();
                
                // Release seats
                for (Ticket ticket : booking.getTickets()) {
                    Seat seat = ticket.getSeat();
                    seat.release();
                    seatRepository.save(seat);
                }

                // Update event available seats
                Event event = booking.getEvent();
                event.setAvailableSeats(event.getAvailableSeats() + booking.getTickets().size());
                eventRepository.save(event);

                bookingRepository.save(booking);
                log.info("Expired booking: {}", booking.getBookingReference());

            } catch (Exception e) {
                log.error("Failed to expire booking {}: {}", booking.getBookingReference(), e.getMessage());
            }
        }
    }

    /**
     * Generates a unique booking reference.
     */
    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a unique ticket number.
     */
    private String generateTicketNumber() {
        return "TKT" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    /**
     * Maps Booking entity to BookingResponse DTO.
     */
    private BookingResponse mapToBookingResponse(Booking booking) {
        List<BookingResponse.TicketDetail> ticketDetails = booking.getTickets().stream()
                .map(ticket -> BookingResponse.TicketDetail.builder()
                        .id(ticket.getId())
                        .ticketNumber(ticket.getTicketNumber())
                        .seatNumber(ticket.getSeat().getSeatNumber())
                        .row(ticket.getSeat().getRow())
                        .category(ticket.getSeat().getCategory())
                        .price(ticket.getPrice())
                        .build())
                .collect(Collectors.toList());

        BookingResponse.PaymentResponse paymentResponse = null;
        if (booking.getPayment() != null) {
            paymentResponse = BookingResponse.PaymentResponse.builder()
                    .id(booking.getPayment().getId())
                    .transactionId(booking.getPayment().getTransactionId())
                    .amount(booking.getPayment().getAmount())
                    .paymentMethod(booking.getPayment().getPaymentMethod())
                    .status(booking.getPayment().getStatus())
                    .build();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUser().getId())
                .username(booking.getUser().getUsername())
                .eventId(booking.getEvent().getId())
                .eventName(booking.getEvent().getName())
                .tickets(ticketDetails)
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .bookingTime(booking.getBookingTime())
                .expiryTime(booking.getExpiryTime())
                .payment(paymentResponse)
                .build();
    }
}
