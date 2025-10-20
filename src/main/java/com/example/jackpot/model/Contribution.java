package com.example.jackpot.model;

import lombok.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Audit record of how a bet contributed to a jackpot's pool at a point in time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contribution implements Serializable {
    private String betId;
    private String userId;
    private String jackpotId;

    // From the bet
    private double stakeAmount;

    // Calculated by contribution strategy
    private double contributionAmount;

    // Pool after applying this contribution
    private double currentJackpotAmount;

    private Instant createdAt;
}
