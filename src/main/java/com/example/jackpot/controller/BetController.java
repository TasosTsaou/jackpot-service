package com.example.jackpot.controller;

import com.example.jackpot.model.Bet;
import com.example.jackpot.model.Reward;
import com.example.jackpot.service.BetService;
import com.example.jackpot.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints to submit bets and evaluate jackpot outcomes.
 */
@RestController
@RequestMapping({"/api", "/api/v1"})
@Tag(name = "Bets", description = "Submit bets and evaluate jackpot outcomes")
public class BetController {
    private final BetService betService;
    private final RewardService rewardService;

    public BetController(BetService betService, RewardService rewardService) {
        this.betService = betService;
        this.rewardService = rewardService;
    }

    @PostMapping("/bets")
    @Operation(
            summary = "Submit a bet",
            description = "Persists the bet and publishes it to Kafka for downstream processing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bet accepted",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Bet published to Kafka: b1"))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = "application/problem+json"))
    })
    public String publishBet(
            @Valid
            @RequestBody(
                    required = true,
                    description = "Bet payload",
                    content = @Content(
                            schema = @Schema(implementation = Bet.class),
                            examples = @ExampleObject(
                                    name = "Sample bet",
                                    value = "{\n  \"betId\": \"b1\",\n  \"userId\": \"u1\",\n  \"jackpotId\": \"J1-FIXED\",\n  \"betAmount\": 75.0\n}")))
            @org.springframework.web.bind.annotation.RequestBody Bet bet) {
        betService.handleBet(bet);
        return "Bet published to Kafka: " + bet.getBetId();
    }

    @GetMapping("/jackpots/{betId}/evaluate")
    @Operation(
            summary = "Evaluate jackpot outcome",
            description = "Returns the reward record for a bet; amount is 0.0 if not a winner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluation result",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Reward.class),
                            examples = @ExampleObject(value = "{\n  \"betId\": \"b1\",\n  \"userId\": \"u1\",\n  \"jackpotId\": \"J1-FIXED\",\n  \"jackpotRewardAmount\": 0.0,\n  \"createdAt\": \"2025-10-17T15:55:31.257Z\"\n}"))),
            @ApiResponse(responseCode = "400", description = "Invalid bet or jackpot",
                    content = @Content(mediaType = "application/problem+json"))
    })
    public Reward evaluate(@PathVariable String betId) {
        return rewardService.evaluateReward(betId);
    }
}
