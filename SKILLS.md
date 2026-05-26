# TTracker — Skills & Technologies

## Backend
- **Java 17** with **Spring Boot**
- **Spring WebFlux** (`WebClient`) for reactive HTTP calls to a live GTFS-RT feed
- **Protocol Buffers** (`protobuf`) for parsing binary GTFS Realtime feed data
- **Spring ResourceLoader** for reading static GTFS files from the classpath

## API Design
- RESTful endpoints with `@RestController`
- Global exception handling via `@RestControllerAdvice`
- JSON responses modelled with Lombok DTOs (`@Data`, `@AllArgsConstructor`)

## Data & Transit
- **GTFS Static** — parsing `stop_times.txt`, `trips.txt`, `calendar_dates.txt`, `stops.txt`, `routes.txt`
- **GTFS Realtime** — overlaying live arrival predictions from the Ilevia Lille RT feed
- Static schedule used as base; real-time data merged by `trip_id` where available
- `parent_station` grouping to surface both directions at a physical stop

## Architecture
- Service-layer separation: `TrackerService` (real-time + merge), `ScheduledTripsService` (static schedule)
- Sibling stop resolution — automatically includes all platforms at the same physical location

## Build
- **Maven** (`mvnw` wrapper)
- **JUnit 5** for unit and integration tests

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/stops?name=` | Search stops by name |
| GET | `/lines?stopId=` | Get upcoming arrivals at a stop (both directions) |
