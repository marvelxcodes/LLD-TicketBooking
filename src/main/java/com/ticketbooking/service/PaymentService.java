package com.ticketbooking.service;

import com.ticketbooking.dto.PaymentRequest;
import com.ticketbooking.dto.PaymentResponse;
import com.ticketbooking.enums.BookingStatus;
import com.ticketbooking.enums.PaymentStatus;
import com.ticketbooking.exception.BookingException;
import com.ticketbooking.exception.PaymentException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Booking;
import com.ticketbooking.model.Payment;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for Payment processing operations.
 * Handles payment processing, refunds, and payment status tracking.
 * Implements simulated payment gateway integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    /**
     * Processes a payment for a booking.
     * Simulates payment gateway integration with 90% success rate.
     * 
     * @param paymentRequest the payment request details
     * @return the processed PaymentResponse
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment for booking: {}", paymentRequest.getBookingId());

        Booking booking = bookingRepository.findById(paymentRequest.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", paymentRequest.getBookingId()));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Only pending bookings can be paid for");
        }

        if (booking.getTotalAmount().compareTo(paymentRequest.getAmount()) != 0) {
            throw new PaymentException("Payment amount does not match booking total");
        }

        // Create payment record
        Payment payment = Payment.builder()
                .transactionId(generateTransactionId())
                .booking(booking)
                .amount(paymentRequest.getAmount())
                .paymentMethod(paymentRequest.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        try {
            // Simulate payment gateway processing
            boolean paymentSuccess = simulatePaymentGateway();

            if (paymentSuccess) {
                payment.success();
                booking.confirm();
                log.info("Payment successful for booking: {}", booking.getBookingReference());
                
                notificationService.sendPaymentConfirmation(booking);
            } else {
                payment.fail("Payment gateway declined transaction");
                log.warn("Payment failed for booking: {}", booking.getBookingReference());
            }

            payment = paymentRepository.save(payment);
            bookingRepository.save(booking);

            return mapToPaymentResponse(payment);

        } catch (Exception e) {
            payment.fail("Payment processing error: " + e.getMessage());
            paymentRepository.save(payment);
            log.error("Payment processing failed: {}", e.getMessage(), e);
            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Processes a refund for a cancelled booking.
     */
    @Transactional
    public PaymentResponse processRefund(Long bookingId) {
        log.info("Processing refund for booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new BookingException("Only cancelled bookings can be refunded");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Only successful payments can be refunded");
        }

        // Simulate refund processing
        try {
            boolean refundSuccess = simulateRefundGateway();

            if (refundSuccess) {
                payment.refund();
                log.info("Refund processed for booking: {}", bookingId);
            } else {
                throw new PaymentException("Refund processing failed");
            }

            payment = paymentRepository.save(payment);
            return mapToPaymentResponse(payment);

        } catch (Exception e) {
            log.error("Refund processing failed: {}", e.getMessage(), e);
            throw new PaymentException("Refund processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves payment details by booking ID.
     */
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        log.info("Fetching payment for booking: {}", bookingId);
        
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));
        
        return mapToPaymentResponse(payment);
    }

    /**
     * Retrieves payment details by transaction ID.
     */
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        log.info("Fetching payment by transaction: {}", transactionId);
        
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));
        
        return mapToPaymentResponse(payment);
    }

    /**
     * Simulates payment gateway processing with 90% success rate.
     * In production, this would integrate with actual payment providers.
     */
    private boolean simulatePaymentGateway() {
        // Simulate 90% success rate
        return Math.random() < 0.9;
    }

    /**
     * Simulates refund gateway processing with 95% success rate.
     */
    private boolean simulateRefundGateway() {
        // Simulate 95% success rate
        return Math.random() < 0.95;
    }

    /**
     * Generates a unique transaction ID.
     */
    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    /**
     * Maps Payment entity to PaymentResponse DTO.
     */
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}
