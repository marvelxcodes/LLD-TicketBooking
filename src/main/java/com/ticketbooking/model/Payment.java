package com.ticketbooking.model;

import com.ticketbooking.enums.PaymentMethod;
import com.ticketbooking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a payment transaction for a booking.
 * Tracks payment details, method, and transaction status.
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String transactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 100)
    private String paymentGatewayResponse;

    @CreationTimestamp
    private LocalDateTime paymentDate;

    /**
     * Marks the payment as successful.
     */
    public void success() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as successful");
        }
        this.status = PaymentStatus.SUCCESS;
    }

    /**
     * Marks the payment as failed.
     */
    public void fail(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as failed");
        }
        this.status = PaymentStatus.FAILED;
        this.paymentGatewayResponse = reason;
    }

    /**
     * Marks the payment as refunded.
     */
    public void refund() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded");
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
