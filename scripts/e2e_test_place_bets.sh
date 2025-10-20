#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_FILE="$PROJECT_DIR/jackpot_output.txt"

echo "Assuming application stack is already running (Kafka, Redis, app)."
echo "Writing jackpot results to $OUTPUT_FILE"
> "$OUTPUT_FILE"

# Test two jackpots from the current seed: 10 bets each
JACKPOTS=("J1-FIXED" "J2-VARIABLE")

for jackpot in "${JACKPOTS[@]}"; do
  echo "--- Placing 10 bets for jackpot: $jackpot ---" | tee -a "$OUTPUT_FILE"
  for i in $(seq 1 10); do
    timestamp=$(date +%s%3N)
    BET_ID="readme-bet-${jackpot}-${timestamp}-${i}"
    USER_ID=$(printf "cli-%s-%02d" "$jackpot" "$i")
    POST_PAYLOAD=$(printf '{"betId":"%s","userId":"%s","jackpotId":"%s","betAmount":%.2f}' "$BET_ID" "$USER_ID" "$jackpot" 75.0)

    echo "Posting bet $BET_ID..."
    curl -sS \
      -H "Content-Type: application/json" \
      -d "$POST_PAYLOAD" \
      http://localhost:8080/api/bets > /dev/null

    sleep 1

    echo "Evaluating jackpot for bet $BET_ID..."
    response=$(curl -sS http://localhost:8080/api/jackpots/"$BET_ID"/evaluate)
    echo "$response"
    echo "$response" >> "$OUTPUT_FILE"
  done
done

echo "Jackpot outcomes written to $OUTPUT_FILE"
cat "$OUTPUT_FILE"
