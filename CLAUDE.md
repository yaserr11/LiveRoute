# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

LiveRoute is a Spring Boot REST backend for location-based scavenger hunts ("gincanas"). A `Gincana` is a hunt tied to a place and coordinates; it owns an ordered list of `Reto` (challenges), each with a clue (`pista`), answer (`respuesta`), and its own GPS point. Domain names, endpoints, and error messages are in Spanish — keep new code consistent with that.

Stack: Spring Boot 4.1.2, Java 25, Maven (wrapper included), Spring Data JPA/Hibernate, MySQL.

## Commands

Use the Maven wrapper (`./mvnw` in Git Bash, `mvnw.cmd` in PowerShell):

```bash
./mvnw spring-boot:run                 # run the API on http://localhost:8080
./mvnw clean package                   # build jar (runs tests)
./mvnw test                            # all tests
./mvnw test -Dtest=LiveRouteApplicationTests              # single test class
./mvnw test -Dtest=LiveRouteApplicationTests#contextLoads # single test method
```

There is no linter configured. `requests.http` contains sample requests runnable from IntelliJ's HTTP client.

## Database setup

- Requires a local MySQL on `localhost:3306` with a `liveroute` database. `LiveRoute.sql` creates the `liveroute_user` account and grants it access (it does not create the database or tables).
- `application.properties` activates the `local` profile; connection details live in `application-local.properties`, which is git-ignored and not tracked. Copy `src/main/resources/application-local.properties.example` to create it.
- Schema is managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`) — tables are created/altered from the entity classes, there are no migration scripts. Changing an entity field changes the DB schema on next startup.
- The only test is a `@SpringBootTest` context-load test, so **tests need the MySQL database running** to pass.

## Architecture

Layered: `controller` → `service` → `repository` → MySQL, with request/response records in `dto`.

- Retos go through `RetoService`: `RetoController` only maps HTTP to service calls. Business errors are thrown from the service as `ResponseStatusException` (404 "Gincana no encontrada" / "Reto no encontrado", 400 "La respuesta es obligatoria").
- Gincanas don't have a service yet: `GincanaController` still calls `GincanaRepository` directly and returns the `Gincana` entity.
- `Reto` has a `@ManyToOne` to `Gincana` (column `gincana_id`). The relationship is unidirectional — `Gincana` has no collection of retos.
- Retos are nested resources under `/api/gincanas/{gincanaId}/retos`; every reto endpoint returns 404 if the gincana doesn't exist.
  - `GET` returns `List<RetoDTO>` (`id, orden, pista, latitud, longitud`) ordered by `orden` via `findByGincanaIdOrderByOrdenAsc`. It never exposes `respuesta` or the parent gincana — player-facing responses must use DTOs, not the entity.
  - `POST` takes a `Reto` entity (without gincana; the service sets it) and returns the full `Reto`, including `respuesta`. This is intentional (admin/creator use).
  - `POST /{retoId}/comprobar` takes `RespuestaRequest {respuesta}` and returns `ResultadoComprobacion {correcto}`. The reto is looked up with `findByIdAndGincanaId`, so a reto from another gincana gives 404. `RetoService.normalizar` compares answers ignoring case, all whitespace and accents (NFD + strip marks, so `ñ` → `n` too).
- DTOs are Java `record`s; entity → DTO mapping lives in a static `desde(...)` factory on the DTO.
- `ResponseStatusException` reasons are not included in error responses by default (`server.error.include-message` isn't set).
- `spring-boot-starter-validation` is on the classpath but no validation annotations are used yet.

## Repo notes

`target/`, `.idea/` and `application-local.properties` are git-ignored and no longer tracked. The DB password from the old `application-local.properties` remains in earlier commits.
