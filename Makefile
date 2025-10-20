SHELL := /bin/bash
COMPOSE ?= docker-compose
GRADLEW := ./gradlew

.PHONY: help up down logs demo clean-output openapi-export

help:
	@echo "Targets:"
	@echo "  test              Run unit & integration tests"
	@echo "  up                Build and start Docker Compose stack"
	@echo "  down              Stop stack and remove volumes"
	@echo "  logs              Follow logs from services"
	@echo "  demo              Run API smoke test against a running stack"
	@echo "  clean-output      Remove jackpot_output.txt"
	@echo "  openapi-export    Export OpenAPI spec to docs/api/openapi.json"

test:
	$(GRADLEW) clean test

up:
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down -v

logs:
	$(COMPOSE) logs -f app kafka redis

demo: clean-output
	bash ./scripts/e2e_test_place_bets.sh

clean-output:
	rm -f jackpot_output.txt

openapi-export:
	@mkdir -p docs/api
	@echo "Exporting OpenAPI spec to docs/api/openapi.json"
	@curl -sS http://localhost:8080/v3/api-docs > docs/api/openapi.json
	@echo "Done: docs/api/openapi.json available at http://localhost:8080/v3/api-docs"
