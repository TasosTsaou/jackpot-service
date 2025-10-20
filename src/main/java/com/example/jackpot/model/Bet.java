package com.example.jackpot.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a bet submitted by a user against a jackpot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Bet", description = "Bet submission payload")
public class Bet {
    @NotBlank
    @Schema(description = "Unique bet identifier", example = "b1")
    private String betId;

    @NotBlank
    @Schema(description = "User placing the bet", example = "u1")
    private String userId;

    @NotBlank
    @Schema(description = "Target jackpot identifier", example = "J1-FIXED")
    private String jackpotId;

    @Positive
    @Schema(description = "Bet amount (> 0)", example = "75.0")
    private double betAmount;
}
