package com.example.jackpot.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Result of evaluating a bet against a jackpot, including payout when applicable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Reward", description = "Jackpot evaluation result")
public class Reward implements Serializable {
    @Schema(description = "Evaluated bet id", example = "b1")
    private String betId;

    @Schema(description = "User id", example = "u1")
    private String userId;

    @Schema(description = "Jackpot id", example = "J1-FIXED")
    private String jackpotId;

    // The amount paid out when a jackpot is won
    @Schema(description = "Payout amount (0.0 if not a winner)", example = "0.0")
    private double jackpotRewardAmount;

    @Schema(description = "Creation timestamp (UTC ISO-8601)", example = "2025-10-17T15:55:31.257Z")
    private Instant createdAt;
}
