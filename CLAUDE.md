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
- `LiveRouteApplicationTests` is a `@SpringBootTest` context-load test, so **`./mvnw test` needs the MySQL database running** to pass. All other tests use mocks (`@WebMvcTest`, Mockito, `MockRestServiceServer`) and need neither MySQL nor network access.

## Architecture

Layered: `controller` → `service` → `repository` → MySQL, with request/response records in `dto`, external HTTP clients in `cliente`, and stateless helpers in `util` (`Texto.normalizar`, `Geo` haversine distance/direction).

- Retos go through `RetoService`: `RetoController` only maps HTTP to service calls. Business errors are thrown from the service as `ResponseStatusException` (404 "Gincana no encontrada" / "Reto no encontrado", 400 "La respuesta es obligatoria").
- `GincanaController`'s plain `GET`/`POST /api/gincanas` still call `GincanaRepository` directly and return the `Gincana` entity; the generation endpoints go through `GeneracionGincanaService`.
- `Reto` has a `@ManyToOne` to `Gincana` (column `gincana_id`). The relationship is unidirectional — `Gincana` has no collection of retos.
- Retos are nested resources under `/api/gincanas/{gincanaId}/retos`; every reto endpoint returns 404 if the gincana doesn't exist.
  - `GET` returns `List<RetoDTO>` (`id, orden, pista, latitud, longitud`) ordered by `orden` via `findByGincanaIdOrderByOrdenAsc`. It never exposes `respuesta` or the parent gincana — player-facing responses must use DTOs, not the entity.
  - `POST` takes a `Reto` entity (without gincana; the service sets it) and returns the full `Reto`, including `respuesta`. This is intentional (admin/creator use).
  - `POST /{retoId}/comprobar` takes `RespuestaRequest {respuesta}` and returns `ResultadoComprobacion {correcto}`. The reto is looked up with `findByIdAndGincanaId`, so a reto from another gincana gives 404. `Texto.normalizar` compares answers ignoring case, all whitespace and accents (NFD + strip marks, so `ñ` → `n` too).
- DTOs are Java `record`s; entity → DTO mapping lives in a static `desde(...)` factory on the DTO.

### Generating gincanas from real places
- `POST /api/gincanas/generar` (`GenerarGincanaRequest`) → 201 `GincanaGeneradaDTO` (gincana fields + `List<RetoDTO>`, never `respuesta`). `GET /api/gincanas/zona/previsualizar?latitud&longitud&radioMetros` → `PrevisualizacionZonaDTO {lugaresDisponibles}`, saves nothing. The backend never assumes a location: center and radius always come from the request.
- Limits and validation messages are constants in `dto/LimitesZona`, shared by the request record's annotations and the preview's `@RequestParam` annotations (Spring built-in method validation — don't add `@Validated` to the controller). radio 200–3000 (default 1000), numeroRetos 1–10 (default 5), dificultad `facil|media|dificil` (default `media`). Defaults are applied in the service, not the record.
- `GeneracionGincanaService.generar`: fetch places → none ⇒ 404 "No se encontraron lugares cerca" → greedy nearest-next route starting from the place nearest the center, cut to `numeroRetos` (fewer if not enough) → zone name → build entities → save gincana + retos in one `TransactionTemplate` block. External calls happen **before** the transaction on purpose.
- `GeneradorPistas`: clue template per `TipoLugar` (never contains the place name, which is the `respuesta`). `facil` adds distance (rounded to 50 m) + direction from the previous point (first reto: from the start point), `media` distance only, `dificil` template only.
- `cliente/`: `ProveedorLugares` (impl `OverpassClient`, Overpass QL `nwr(around:…)` for named historic/tourism/amenity/park places, `out center`, dedupe by normalized name, any `RestClientException` ⇒ `ServicioMapasException`) and `GeocodificadorInverso` (impl `NominatimClient`, returns `Optional.empty()` on any failure; then lugar is `"Zona <lat>, <lon>"`). Shared setup in `ClientesHttp`: connect 5 s / read 30 s timeouts, `User-Agent: LiveRoute/1.0`. URLs come from `overpass.url` / `nominatim.url` in `application.properties`. Each client has a package-private constructor taking a `RestClient.Builder` for `MockRestServiceServer` tests. Spring Boot's `RestClient.Builder` bean is not on the classpath — clients build their own.

### Errors
- `controller/ManejadorErrores` (`@RestControllerAdvice`) turns every handled error into `{"error": "<mensaje>"}`: validation/bad JSON/bad or missing params → 400 (multiple messages sorted and joined with ". "), `ResponseStatusException` → its status + reason, `ServicioMapasException` → 502 "Servicio de mapas no disponible" (logged at WARN with the cause).

## Repo notes

`target/`, `.idea/` and `application-local.properties` are git-ignored and no longer tracked. The DB password from the old `application-local.properties` remains in earlier commits.

Overpass and Nominatim are free public services with rate limits (Nominatim ~1 req/s) and occasional outages; the app should debounce `previsualizar` while the player changes the radius. To test against a different Overpass instance: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--overpass.url=<url>`.
