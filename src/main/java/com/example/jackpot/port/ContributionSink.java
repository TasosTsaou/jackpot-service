package com.example.jackpot.port;

import com.example.jackpot.model.Contribution;

/**
 * Outbound port for recording contribution events.
 */
public interface ContributionSink {
    void appendContribution(Contribution contribution);
}

