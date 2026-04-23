package com.ticketbooking.dto;

import com.ticketbooking.enums.SeatCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single seat selection in a booking request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSelection {

    @NotBlank(message = "Seat number is required")
    private String seatNumber;

    @NotBlank(message = "Row is required")
    private String row;

    @NotNull(message = "Seat category is required")
    private SeatCategory category;
}
