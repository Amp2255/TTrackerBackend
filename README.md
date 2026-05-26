# TTracker

A Spring Boot REST API for real-time transit tracking in Lille (France), powered by the Ilevia GTFS feed.

## What it does

TTracker lets you look up bus/tram stops by name and get upcoming arrival times for each stop. It merges static GTFS schedule data with live GTFS-RT updates from the [transport.data.gouv.fr](https://transport.data.gouv.fr) proxy, so results reflect real-time delays when available.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/stops?name=<query>` | Search stops by name |
| GET | `/lines?stopId=<id>` | Get upcoming arrivals for a stop |

## Stack

- Java 21, Spring Boot 3.5
- WebFlux (WebClient) for async HTTP
- GTFS Realtime (protobuf) via `gtfs-realtime-bindings`
- Static GTFS data bundled as classpath resources

## Running

```bash
./mvnw spring-boot:run
```
