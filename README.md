# Insurance API

A Spring Boot REST API that acts as a **middleware** between a client (e.g. a frontend or Postman) and an external mock API running on WireMock. It does not store any data itself — it fetches everything from WireMock and returns it to the caller.

---

## Project structure

```
src/main/java/com/example/insurance/
├── InsuranceApplication.java
├── client/
│   ├── ClaimClient.java
│   └── PolicyClient.java
├── config/
│   ├── OpenApiConfig.java
│   ├── RestClientConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ClaimController.java
│   ├── GlobalExceptionHandler.java
│   ├── PolicyController.java
│   └── UserController.java
├── exceptions/
│   ├── ResourceNotFoundException.java
│   └── UpstreamServiceException.java
├── external/
│   ├── ExternalClaim.java
│   └── ExternalPolicy.java
├── mappers/
│   ├── ClaimMapper.java
│   └── PolicyMapper.java
├── model/
│   ├── Claim.java
│   └── Policy.java
├── security/
│   ├── JwtProperties.java
│   ├── JwtService.java
│   └── TokenStore.java
└── services/
    ├── ClaimService.java
    ├── PolicyCacheService.java
    └── PolicyService.java
```

---

## Step 1 — Entry point

`InsuranceApplication.java` bootstraps the entire Spring context via `@SpringBootApplication`. It automatically scans all classes under `com.example.insurance` and registers them.

---

## Step 2 — Configuration

### `RestClientConfig.java`
Creates a `RestClient` bean pointed at the WireMock base URL. The URL is externalised via `@Value` and read from `application.yml`:

```yaml
app:
  wiremock:
    base-url: http://localhost:8081
```

This is the HTTP client used to call the external API. By declaring it as a `@Bean`, Spring can inject it wherever needed.

### `SecurityConfig.java`
Secures all endpoints using **JWT Bearer tokens** via Spring's OAuth2 Resource Server. The following paths are public (no token required):

| Path | Reason |
|---|---|
| `POST /auth/login` | Entry point to obtain a token |
| `POST /auth/refresh` | Obtain a new access token using a refresh token |
| `POST /auth/logout` | Invalidate the current token |
| `/swagger-ui/**`, `/v3/api-docs/**` | API documentation |
| `/actuator/**` | Health and metrics endpoints |

Any other request without a valid JWT gets a `401 Unauthorized` automatically.

The `JwtDecoder` bean validates the token signature (HMAC-SHA256) and checks the token against a blacklist on every request. If the token has been revoked via logout, a `401` is returned.

### `OpenApiConfig.java`
Configures Swagger UI to show an **Authorize** button, allowing you to paste your JWT token and test secured endpoints directly from the browser.

---

## Step 3 — Security

### `JwtService.java`
Handles token creation. When a user logs in, it generates:
- An **access token** (JWT signed with HMAC-SHA256, expires in 1 hour) — contains `sub` (username) and `jti` (unique token ID for blacklisting)
- A **refresh token** (opaque UUID, expires in 7 days) — stored in `TokenStore`

### `JwtProperties.java`
Type-safe configuration binding for JWT settings via `@ConfigurationProperties(prefix = "app.jwt")`:

```yaml
app:
  jwt:
    secret: "insurance-application-jwt-secret-key-2026"
    expiration: PT1H
    refresh-expiration: P7D
```

### `TokenStore.java`
In-memory store with two responsibilities:
- **Refresh tokens** — maps opaque UUIDs to username + expiry time
- **Blacklist** — stores JTI values of revoked access tokens (populated on logout)

---

## Step 4 — Models

Two pairs of classes exist — one for each resource:

- **`Policy` / `ExternalPolicy`** — an insurance policy with `policyId`, `description`, `coverages`
- **`Claim` / `ExternalClaim`** — a claim with `claimId`, `description`, `status`, `date`

The `External` versions represent what WireMock returns. Since WireMock responds with Spanish field names (`polizaId`, `descripcion`, etc.), the `External` classes use `@JsonProperty` to map those JSON keys to the English field names. The non-external versions are what this API returns to the client. Separating them means the response shape can change independently of what WireMock returns. Lombok generates getters, setters, and `toString()` automatically.

---

## Step 5 — Services

This is where the actual logic lives. Services call WireMock via `RestClient` and map the results.

### `PolicyService.java`

| Method | What it does |
|---|---|
| `getPoliciesByNationalId(nationalId)` | Delegates to `PolicyCacheService` to get all policies for the user |
| `getPolicyById(policyId, nationalId)` | Verifies ownership, then calls `GET /polizas/{policyId}` on WireMock |
| `getConditions(policyId, nationalId)` | Verifies ownership, then calls `GET /polizas/{policyId}/condiciones` on WireMock |
| `getClaims(policyId, nationalId)` | Verifies ownership, then calls `GET /polizas/{policyId}/siniestros` on WireMock |
| `checkOwnership(policyId, nationalId)` | Fetches all the user's policies and checks the requested one belongs to them. Throws `403` if not |

### `ClaimService.java`

| Method | What it does |
|---|---|
| `getClaimById(claimId)` | Calls `GET /siniestros/{claimId}` on WireMock |

### `PolicyCacheService.java`

The cached logic lives in a dedicated service rather than in `PolicyService`. This is necessary because Spring caching works through proxies — if a method calls another method within the same class, the proxy is bypassed and the cache is skipped.

---

## Step 6 — Controllers

Controllers receive HTTP requests and delegate to services. They extract the authenticated user's username from the `Jwt` principal injected by Spring Security.

### `AuthController.java` — base path `/auth`

| Endpoint | What it does |
|---|---|
| `POST /auth/login` | Accepts `{"username":"..."}`, returns `accessToken` + `refreshToken` |
| `POST /auth/refresh` | Accepts `{"refreshToken":"..."}`, returns a new `accessToken` |
| `POST /auth/logout` | Accepts `{"refreshToken":"..."}`, blacklists the current access token server-side |

### `UserController.java` — base path `/user`

| Endpoint | What it does |
|---|---|
| `GET /user` | Returns `{"name":"<username>"}` for the authenticated user |

### `PolicyController.java` — base path `/policies`

| Endpoint | What it does |
|---|---|
| `GET /policies` | Gets all policies for the logged-in user |
| `GET /policies/{policyId}` | Gets one policy (ownership verified) |
| `GET /policies/{policyId}/conditions` | Gets the policy's conditions (ownership verified) |
| `GET /policies/{policyId}/claims` | Gets claims linked to a policy (ownership verified) |

### `ClaimController.java` — base path `/claims`

| Endpoint | What it does |
|---|---|
| `GET /claims/{claimId}` | Gets a single claim by ID |

---

## Step 7 — Error handling

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
| `RequestNotPermitted` | `429 Too Many Requests` |

---

## Step 8 — Caching

### Why caching is needed

Every request that requires ownership verification calls `getPoliciesByNationalId` to fetch all policies for the authenticated user. Without caching, this results in a WireMock call on every single request — even when the data hasn't changed.

### How it works

Spring Boot's built-in caching is enabled with `@EnableCaching` on `InsuranceApplication`. The `@Cacheable` annotation on a method stores the result the first time it is called. On subsequent calls with the same arguments, the stored result is returned directly without calling WireMock.

| Method | Cache name | Cache key |
|---|---|---|
| `getPoliciesByNationalId(nationalId)` | `policies` | `nationalId` |

### Cache flow

```
First call  → cache miss  → calls WireMock → stores result → returns result
Second call → cache hit   → returns stored result (WireMock not called)
```

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

Caffeine is used as the cache provider. The TTL is configured in `application.yml`:

```yaml
spring:
  cache:
    caffeine:
      spec: expireAfterWrite=60s
```

---

## Step 9 — Circuit breaking

### Why circuit breaking is needed

If WireMock is down or slow, without a circuit breaker every request would hang until a timeout, degrading the entire service. A circuit breaker detects repeated failures and short-circuits requests, returning a fallback immediately instead of waiting.

### How it works

Resilience4j's `@CircuitBreaker` annotation is applied to service methods. Each annotated method declares a `fallbackMethod` that is called when the circuit is open or the call fails. The fallback throws an `UpstreamServiceException`, which is then handled by `GlobalExceptionHandler` and returned as a `502 Bad Gateway`.

### Circuit breaker configuration (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      wiremock:
        failure-rate-threshold: 50
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
```

| Property | Value | Meaning |
|---|---|---|
| `failure-rate-threshold` | `50` | Opens the circuit when 50% of calls fail |
| `minimum-number-of-calls` | `5` | Requires at least 5 calls before it can open |
| `wait-duration-in-open-state` | `10s` | Waits 10 seconds before trying again (half-open) |
| `permitted-number-of-calls-in-half-open-state` | `3` | Allows 3 test calls in half-open state |

### Circuit breaker states

```
CLOSED (normal) ──── failure rate > 50% ──→ OPEN (rejects all calls)
                                                │
                                          after 10s
                                                │
                                                ▼
                                         HALF-OPEN (3 test calls)
                                          │             │
                                       success        failure
                                          │             │
                                          ▼             ▼
                                       CLOSED         OPEN
```

---

## Step 10 — Rate limiting

Login attempts are rate-limited using Resilience4j's `@RateLimiter`. The `login` endpoint allows a maximum of **5 requests per minute per instance**. Exceeding the limit returns `429 Too Many Requests`.

```yaml
resilience4j:
  ratelimiter:
    instances:
      login:
        limit-for-period: 5
        limit-refresh-period: 1m
        timeout-duration: 0
```

---

## Step 11 — Actuator & metrics

Spring Boot Actuator exposes health and metrics endpoints:

| Endpoint | What it shows |
|---|---|
| `GET /actuator/health` | Application health + circuit breaker state |
| `GET /actuator/metrics` | Available metric names |
| `GET /actuator/prometheus` | Prometheus-format metrics (for scraping) |
| `GET /actuator/info` | Application info |

---

## Request flow

```
Client
  │
  ▼
Spring Security ──── no/invalid token ──→ 401
  │
  ▼
Rate Limiter (login only) ──── limit exceeded ──→ 429
  │
  ▼
Controller  (extracts username from Jwt principal)
  │
  ▼
Service  (checkOwnership if needed) ──── not owner ──→ 403
  │
  ▼
Circuit Breaker ──── circuit open ──→ UpstreamServiceException ──→ 502
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
3. Obtain a JWT token and use it in subsequent requests

```bash
# 1. Login — get an access token and a refresh token
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}'

# 2. Export the access token for reuse
export TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}' | jq -r '.accessToken')

# 3. Get all policies for the logged-in user
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/policies

# 4. Get a specific policy
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/policies/{policyId}

# 5. Get policy conditions
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/policies/{policyId}/conditions

# 6. Get claims for a policy
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/policies/{policyId}/claims

# 7. Get a specific claim
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/claims/{claimId}

# 8. Refresh access token
curl -s -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<your-refresh-token>"}'

# 9. Logout (blacklists the current access token)
curl -s -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<your-refresh-token>"}'
```

## API Documentation (Swagger UI)

With the application running, open:

```
http://localhost:8080/swagger-ui.html
```

Click **Authorize**, paste your JWT token, and test all endpoints directly from the browser.
