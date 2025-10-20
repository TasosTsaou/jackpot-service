package com.example.jackpot.controller;

import com.example.jackpot.service.BetService;
import com.example.jackpot.service.RewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { BetController.class, GlobalExceptionHandler.class })
class BetControllerErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BetService betService;

    @MockBean
    private RewardService rewardService;

    @Test
    @DisplayName("POST /api/bets with missing betId returns 400 ProblemDetail")
    void postBet_missingBetId_returns400Problem() throws Exception {
        String payload = "{\n" +
                "  \"userId\": \"u1\",\n" +
                "  \"jackpotId\": \"J1-FIXED\",\n" +
                "  \"betAmount\": 75.0\n" +
                "}";

        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation failed"));

        verify(betService, never()).handleBet(any());
    }

    @Test
    @DisplayName("POST /api/bets with non-positive amount returns 400 ProblemDetail")
    void postBet_nonPositiveAmount_returns400Problem() throws Exception {
        String payload = "{\n" +
                "  \"betId\": \"b1\",\n" +
                "  \"userId\": \"u1\",\n" +
                "  \"jackpotId\": \"J1-FIXED\",\n" +
                "  \"betAmount\": 0.0\n" +
                "}";

        mockMvc.perform(post("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation failed"));

        verify(betService, never()).handleBet(any());
    }

    @Test
    @DisplayName("GET /api/jackpots/{betId}/evaluate when service throws returns 400 ProblemDetail")
    void evaluate_illegalArgument_returns400Problem() throws Exception {
        when(rewardService.evaluateReward(eq("nope")))
                .thenThrow(new IllegalArgumentException("Bet not found: nope"));

        mockMvc.perform(get("/api/jackpots/nope/evaluate"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail", containsString("Bet not found")));
    }
}
