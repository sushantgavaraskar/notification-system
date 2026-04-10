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
- demo/ : PowerShell scripts to start, trigger, and stop the live demo

## Pipeline Topics

- raw-events
- enriched-events
- actionable-notifications
- delivery-stream

## Quick Demo (Recommended)

Run these commands from the project root (`d:\major project`):

1. Start infra + all services:

   `powershell -ExecutionPolicy Bypass -File .\demo\start-demo.ps1`

2. Open UIs:

- Delivery dashboard: http://localhost:8085
- Kafka UI: http://localhost:8088

3. Validate demo readiness:

   `powershell -ExecutionPolicy Bypass -File .\\demo\\check-demo-health.ps1 -ShowLogHints`

4. Trigger a scenario with built-in readiness wait (single command):

   `powershell -ExecutionPolicy Bypass -File .\demo\run-demo-scenario.ps1 -Scenario full-story -UserId user-1 -WaitForReady`

5. Or trigger immediately if stack is already ready:

   `powershell -ExecutionPolicy Bypass -File .\demo\run-demo-scenario.ps1 -Scenario full-story -UserId user-1`

6. Stop everything after demo:

   `powershell -ExecutionPolicy Bypass -File .\demo\stop-demo.ps1`

Available scenarios:

- message-burst
- battery-low
- user-inactive
- critical-alert
- full-story

## Manual Run (Alternative)

1. Start infrastructure:

   `docker compose -f infra/docker-compose.yml up -d`

2. Verify services:

- Kafka UI: http://localhost:8088
- Redis: localhost:6379
- PostgreSQL: localhost:5432

3. Start services in this order:

- event-producer
- context-engine
- decision-engine
- notification-orchestrator
- delivery-service

4. Trigger demo traffic:

   `POST http://localhost:8081/api/events/demo/full-story?userId=user-1`

## Demo Flow For Presentation

1. Explain architecture quickly using the topic chain:

   `raw-events -> enriched-events -> actionable-notifications -> delivery-stream`

2. Show dashboard (`http://localhost:8085`) and Kafka UI (`http://localhost:8088`).

3. Trigger a scenario using script or Postman.

4. Point out live delivery on dashboard (WebSocket push from `delivery-service`).

5. Optional resilience demo:

- stop `delivery-service`
- trigger scenarios
- start `delivery-service` again
- show backlog consumption from Kafka

## Current Phase

Phase 1 and Phase 2 scaffolding with end-to-end flow are implemented and demo-ready.
