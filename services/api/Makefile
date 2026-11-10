.DEFAULT_GOAL := run

ifeq ($(OS),Windows_NT)
SHELL := cmd.exe
.SHELLFLAGS := /c
GRADLE := .\gradlew.bat
else
GRADLE := ./gradlew
endif

COMPOSE := docker compose -f compose.yaml

.PHONY: run generate-openapi db-up db-down db-logs

run: db-up
	$(GRADLE) bootRun --args="--spring.profiles.active=local"

generate-openapi: db-up
	node scripts/generate-openapi.mjs

db-up:
	$(COMPOSE) up --detach --wait --wait-timeout 120 postgres

db-down:
	$(COMPOSE) down

db-logs:
	$(COMPOSE) logs --follow postgres
