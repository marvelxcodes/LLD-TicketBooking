package com.ticketbooking.dto;

import com.ticketbooking.enums.SeatCategory;
import com.ticketbooking.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for seat information in responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String row;
    private SeatCategory category;
    private BigDecimal price;
    private SeatStatus status;
}
