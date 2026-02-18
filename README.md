# Polizas API

A Spring Boot REST API that acts as a **middleware** between a client (e.g. a frontend or Postman) and an external mock API running on WireMock. It does not store any data itself — it fetches everything from WireMock and returns it to the caller.

---

## Project structure

```
src/main/java/com/example/polizas/
├── PolizasApplication.java
├── config/
│   ├── RestClientConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── GlobalExceptionHandler.java
│   ├── PolizasController.java
│   └── SiniestrosController.java
├── model/
│   ├── Poliza.java
│   ├── PolizaExternal.java
│   ├── Siniestro.java
│   ├── SiniestroExternal.java
│   ├── ResourceNotFoundException.java
│   └── UpstreamServiceException.java
└── services/
    ├── PolizasService.java
    └── SiniestrosService.java
```

---

## Step 1 — Entry point

`PolizasApplication.java` bootstraps the entire Spring context via `@SpringBootApplication`. It automatically scans all classes under `com.example.polizas` and registers them.

---

## Step 2 — Configuration

### `RestClientConfig.java`
Creates a `RestClient` bean pointed at `http://localhost:8081` (WireMock). This is the HTTP client used to call the external API. By declaring it as a `@Bean`, Spring can inject it wherever needed.

### `SecurityConfig.java`
Adds authentication to all endpoints using **HTTP Basic Auth**. Two in-memory users are defined, using their DNI as username:

| Username | Password |
|---|---|
| `12345678A` | `password` |
| `87654321B` | `password` |

Any request without valid credentials gets a `401 Unauthorized` automatically.

---

## Step 3 — Models

Two pairs of classes exist — one for each resource:

- **`Poliza` / `PolizaExternal`** — an insurance policy with `polizaId`, `descripcion`, `coberturas`
- **`Siniestro` / `SiniestroExternal`** — a claim with `siniestroId`, `descripcion`, `estado`, `fecha`

The `External` versions represent what WireMock returns. The non-external versions are what this API returns to the client. Separating them means the response shape can change independently of what WireMock returns. Lombok generates getters, setters, and `toString()` automatically.

---

## Step 4 — Services

This is where the actual logic lives. Services call WireMock via `RestClient` and map the results.

### `PolizasService.java`

| Method | What it does |
|---|---|
| `getPolizasByDni(dni)` | Calls `GET /polizas?dni={dni}` on WireMock, returns all policies for that user |
| `getPolizaById(polizaId, dni)` | Verifies ownership, then calls `GET /polizas/{polizaId}` |
| `getCondiciones(polizaId, dni)` | Verifies ownership, then calls `GET /polizas/{polizaId}/condiciones` |
| `getSiniestros(polizaId, dni)` | Verifies ownership, then calls `GET /polizas/{polizaId}/siniestros` |
| `checkOwnership(polizaId, dni)` | Fetches all the user's policies and checks the requested one belongs to them. Throws `403` if not |

The `toPoliza()` and `toSiniestro()` private methods map from the `External` DTOs to the internal models.

### `SiniestrosService.java`

| Method | What it does |
|---|---|
| `getSiniestroById(siniestroId)` | Calls `GET /siniestros/{siniestroId}` on WireMock |

---

## Step 5 — Controllers

Controllers receive HTTP requests and delegate to services. They also extract the authenticated user's identity from `Principal`.

### `PolizasController.java` — base path `/polizas`

| Endpoint | What it does |
|---|---|
| `GET /polizas` | Gets all policies for the logged-in user |
| `GET /polizas/{polizaId}` | Gets one policy (ownership verified) |
| `GET /polizas/{polizaId}/condiciones` | Gets the policy's conditions (ownership verified) |
| `GET /polizas/{polizaId}/siniestros` | Gets claims linked to a policy (ownership verified) |

`Principal` is injected by Spring Security automatically — it represents the logged-in user. `principal.getName()` returns the username, which in this case is the DNI.

### `SiniestrosController.java` — base path `/siniestros`

| Endpoint | What it does |
|---|---|
| `GET /siniestros/{siniestroId}` | Gets a single claim by ID |

---

## Step 6 — Error handling

### Custom exceptions

- **`UpstreamServiceException`** — thrown when WireMock is unreachable or returns an error
- **`ResourceNotFoundException`** — thrown when WireMock returns a 404

### `GlobalExceptionHandler.java`

A `@RestControllerAdvice` that intercepts exceptions thrown anywhere in the app and returns a clean JSON response instead of a stack trace:

| Exception | HTTP status returned |
|---|---|
| `ResourceNotFoundException` | `404 Not Found` |
| `UpstreamServiceException` | `502 Bad Gateway` |
| `ResponseStatusException` | Whatever status the exception carries (e.g. `403`) |

---

## Request flow

```
Client
  │
  ▼
Spring Security ──── no credentials ──→ 401
  │
  ▼
Controller  (extracts DNI from Principal)
  │
  ▼
Service  (checkOwnership if needed) ──── not owner ──→ 403
  │
  ▼
RestClient ──── WireMock down ──→ UpstreamServiceException ──→ 502
  │              WireMock 404 ──→ ResourceNotFoundException ──→ 404
  ▼
WireMock (external mock API on port 8081)
  │
  ▼
Response mapped and returned to client as JSON
```

---

## Running the project

1. Start WireMock on port `8081`
2. Run the Spring Boot application on port `8080`
3. Make authenticated requests using HTTP Basic Auth

```bash
# Get all policies for the logged-in user
curl -u 12345678A:password http://localhost:8080/polizas

# Get a specific policy
curl -u 12345678A:password http://localhost:8080/polizas/{polizaId}

# Get policy conditions
curl -u 12345678A:password http://localhost:8080/polizas/{polizaId}/condiciones

# Get claims for a policy
curl -u 12345678A:password http://localhost:8080/polizas/{polizaId}/siniestros

# Get a specific claim
curl -u 12345678A:password http://localhost:8080/siniestros/{siniestroId}
```
