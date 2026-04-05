# Context-Aware Distributed Notification Orchestration System

A distributed event-driven notification platform that sends the right notification at the right time using user context, prioritization, deduplication, and intelligent orchestration.

## Stack

- Java 21 + Spring Boot
- Apache Kafka
- Redis
- PostgreSQL
- Docker Compose
- WebSocket + Vanilla JS dashboard

## Project Layout

- infra/ : Docker Compose for Kafka, Redis, PostgreSQL
- event-producer/ : Simulates and publishes events
- context-engine/ : Builds user context and emits enriched events
- decision-engine/ : Applies suppression, deduplication, and rate limiting
- notification-orchestrator/ : Decides timing and channel
- delivery-service/ : Streams final notifications to WebSocket dashboard

## Run Infrastructure

1. Start infrastructure:

   docker compose -f infra/docker-compose.yml up -d

2. Verify services:

- Kafka UI: http://localhost:8088
- Redis: localhost:6379
- PostgreSQL: localhost:5432

## Pipeline Topics

- raw-events
- enriched-events
- actionable-notifications
- delivery-stream

## Current Phase

Phase 1 and Phase 2 scaffolding with end-to-end flow are in progress.
