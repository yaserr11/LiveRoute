# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

LiveRoute is a Spring Boot REST backend (plus a React web front end in `frontend/`) for location-based scavenger hunts ("gincanas"). A `Gincana` is a hunt tied to a place and coordinates; it owns an ordered list of `Reto` (challenges), each with a clue (`pista`), answer (`respuesta`), and its own GPS point. Domain names, endpoints, and error messages are in Spanish — keep new code consistent with that.

Stack: Spring Boot 4.1.2, Java 25, Maven (wrapper included), Spring Data JPA/Hibernate, MySQL. Front end: React 19 + Vite 8 (JavaScript), react-leaflet 5, react-router-dom 7, Node 24.

## Commands

Use the Maven wrapper (`./mvnw` in Git Bash, `mvnw.cmd` in PowerShell):

```bash
./mvnw spring-boot:run                 # run the API on http://localhost:8080
./mvnw clean package                   # build jar (runs tests)
./mvnw test                            # all tests
./mvnw test -Dtest=LiveRouteApplicationTests              # single test class
./mvnw test -Dtest=LiveRouteApplicationTests#contextLoads # single test method
```

Front end (from `frontend/`, needs the backend running):

```bash
cp .env.example .env    # first time only; VITE_API_URL points at the backend
npm install             # first time / after dependency changes
npm run dev             # http://localhost:5173 (strictPort: CORS only allows this origin)
npm run build           # production build into frontend/dist — use it as the front end's "compile check"
```

There is no linter configured (backend or front end) and no front-end tests. `requests.http` contains sample requests runnable from IntelliJ's HTTP client.

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
- `cliente/`: `ProveedorLugares` (impl `OverpassClient`, Overpass QL `nwr(around:…)` for named historic/tourism/amenity/park places, `out center`, dedupe by normalized name) and `GeocodificadorInverso` (impl `NominatimClient`, returns `Optional.empty()` on any failure; then lugar is `"Zona <lat>, <lon>"`). Shared setup in `ClientesHttp`: connect 5 s / read 30 s timeouts **per server**, `User-Agent: LiveRoute/1.0`. Each client has a package-private constructor taking a `RestClient.Builder` (and for Overpass a `Clock`) for `MockRestServiceServer` tests. Spring Boot's `RestClient.Builder` bean is not on the classpath — clients build their own.
- **Overpass failover:** `overpass.urls` (comma-separated list, main server first) in `application.properties`; `nominatim.url` is a single URL. `OverpassClient` tries servers in order and moves to the next on any `RestClientException` — HTTP error (502/429/5xx), timeout, non-JSON body, or a 200 whose `remark` contains `error` (Overpass's "runtime error: Query timed out", i.e. partial result, via `RespuestaIncompletaException`). Only when **all** fail does it throw `ServicioMapasException` (→ 502), with the other failures as suppressed exceptions. A server that fails is paused for `PAUSA_TRAS_FALLO` (2 min, in-memory per instance): during the pause it is tried last, so requests don't wait for its timeout again; a success clears the pause. Every request logs `Overpass respondió desde <url> en N ms (M lugares)` (INFO) or `Overpass falló en <url> …` (WARN, first line of the error only).
- **Choosing mirrors:** test with Java, not curl — curl uses the Windows certificate store, Java uses its own truststore (e.g. `maps.mail.ru` works in curl but fails in Java with `PKIX path building failed`). Also check the mirror has worldwide data, not just one country. As of 2026-11-16: `overpass-api.de` (main, often overloaded), `overpass.openstreetmap.fr` (fast, worldwide), `lz4.overpass-api.de` (answers but slow); `overpass.kumi.systems` and `overpass.private.coffee` were not answering.

### Errors
- `controller/ManejadorErrores` (`@RestControllerAdvice`) turns every handled error into `{"error": "<mensaje>"}`: validation/bad JSON/bad or missing params → 400 (multiple messages sorted and joined with ". "), `ResponseStatusException` → its status + reason, `ServicioMapasException` → 502 "Servicio de mapas no disponible" (logged at WARN with the cause).

### CORS
- `config/CorsConfig` allows `/api/**` (GET, POST, `Content-Type` header) only from `liveroute.cors.origenes` in `application.properties` (comma-separated, default `http://localhost:5173`). Add origins there (e.g. the future mobile app) instead of touching code; override at run time with `--liveroute.cors.origenes=<a>,<b>`. `CorsConfigTest` checks allowed/preflight/rejected origins.

## Front end (`frontend/`)

Mobile-first web app, UI text in Spanish, light/dark via `prefers-color-scheme`, all styles in `src/index.css` (CSS variables, BEM-ish Spanish class names, no UI framework).

- `src/api/config.js` is the **only** place that reads `VITE_API_URL` (from `.env`, git-ignored; `.env.example` is tracked). `src/api/cliente.js` has every API call; it throws `Error(<backend "error" message>)` with `.status`, or a "No se puede conectar con el servidor" message on network failure — screens show `e.message` as-is.
- Folders: `pantallas/` (one per route), `components/` (small presentational pieces), `hooks/` (`useGeolocalizacion`, `usePosicionEnVivo`, `useDebounce`), `util/` (`formato`, `progreso`).
- Routes (`App.jsx`, `BrowserRouter`), with a bottom `BarraNavegacion`:
  - `/` **Inicio**: geolocation (fallback Puerto de Sagunto 39.6603, -0.2386), draggable marker / tap to move, radius slider 200–3000 m drawn as a circle. `previsualizar` is called 600 ms after the last change (`useDebounce`), with `AbortController` cancelling stale requests. The zone object is `useMemo`'d — a new object each render would restart the debounce forever. "Continuar" passes `{zona, lugares}` to Generar via router state.
  - `/generar` **Generar**: numeroRetos 1–10, dificultad. Shows a spinner while `POST /generar` runs (can take 30 s+ when the main Overpass server times out), then `navigate('/jugar/{id}', {replace: true, state: {gincana}})`.
  - `/jugar/:id` **Jugar**: retos from `GET .../retos`. The gincana (title and start point) comes from router state, or else is found in `GET /api/gincanas` (there is no GET-by-id). The current reto index is stored in `localStorage` key `liveroute.progreso.{id}` (`util/progreso.js`, all access in try/catch). `TarjetaReto` is keyed by reto id so its state resets per reto. Shows `PantallaFinal` when the index ≥ number of retos.
  - `/gincanas` **Mis gincanas**: newest first. "Jugar" clears that gincana's progress (replay starts at reto 1).
- **Never reveal answers in the UI.** The API never sends `respuesta` to the player. `MapaJuego` only draws the player's live position and the *previous* point (start point for reto 1, otherwise the solved reto) — never the current reto's coordinates, which would give the place away. A "warmer/colder" distance hint is a possible future optional feature; it should be computed so the target position is still not exposed in the UI.
- Map tiles: `https://tile.openstreetmap.org/{z}/{x}/{y}.png` (`CAPA_OSM` in `MapaZona.jsx`). Markers are CSS `divIcon`s (`components/iconosMapa.js`) because Leaflet's default image icons don't resolve under Vite.

## Repo notes

`target/`, `.idea/`, `application-local.properties`, `frontend/node_modules`, `frontend/dist` and `frontend/.env` are git-ignored and no longer tracked. The DB password from the old `application-local.properties` remains in earlier commits.

Overpass and Nominatim are free public services with rate limits (Nominatim ~1 req/s) and occasional outages; the app should debounce `previsualizar` while the player changes the radius. To try different Overpass servers without editing the file: `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--overpass.urls=<url1>,<url2>"`. Add `--server.port=8081` if the app is already running from IntelliJ on 8080.
