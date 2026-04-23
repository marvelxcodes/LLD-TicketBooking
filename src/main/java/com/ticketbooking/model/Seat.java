package com.ticketbooking.model;

import com.ticketbooking.enums.SeatCategory;
import com.ticketbooking.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a seat in an event venue.
 * Each seat has a category, price, and current status.
 */
@Entity
@Table(name = "seats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String seatNumber;

    @Column(nullable = false, length = 5)
    private String row;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SeatCategory category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Locks the seat for a temporary period during booking.
     */
    public void lock() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available for locking");
        }
        this.status = SeatStatus.LOCKED;
    }

    /**
     * Confirms the seat booking.
     */
    public void book() {
        if (this.status != SeatStatus.LOCKED) {
            throw new IllegalStateException("Seat must be locked before booking");
        }
        this.status = SeatStatus.BOOKED;
    }

    /**
     * Releases a locked seat back to available status.
     */
    public void release() {
        if (this.status != SeatStatus.LOCKED) {
            throw new IllegalStateException("Seat is not locked for release");
        }
        this.status = SeatStatus.AVAILABLE;
    }
}
