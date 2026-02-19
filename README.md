# Insurance API

A Spring Boot REST API that acts as a **middleware** between a client (e.g. a frontend or Postman) and an external mock API running on WireMock. It does not store any data itself — it fetches everything from WireMock and returns it to the caller.

---

## Project structure

```
src/main/java/com/example/insurance/
├── InsuranceApplication.java
├── config/
│   ├── RestClientConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── GlobalExceptionHandler.java
│   ├── PolicyController.java
│   └── ClaimController.java
├── exceptions/
│   ├── ResourceNotFoundException.java
│   └── UpstreamServiceException.java
├── model/
│   ├── Policy.java
│   ├── ExternalPolicy.java
│   ├── Claim.java
│   └── ExternalClaim.java
└── services/
    ├── PolicyCacheService.java
    ├── PolicyService.java
    └── ClaimService.java
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
Adds authentication to all endpoints using **HTTP Basic Auth**. Two in-memory users are defined, using their national ID as username:

| Username | Password |
|---|---|
| `12345678A` | `password` |
| `87654321B` | `password` |

Any request without valid credentials gets a `401 Unauthorized` automatically.

---

## Step 3 — Models

Two pairs of classes exist — one for each resource:

- **`Policy` / `ExternalPolicy`** — an insurance policy with `policyId`, `description`, `coverages`
- **`Claim` / `ExternalClaim`** — a claim with `claimId`, `description`, `status`, `date`

The `External` versions represent what WireMock returns. Since WireMock responds with Spanish field names (`polizaId`, `descripcion`, etc.), the `External` classes use `@JsonProperty` to map those JSON keys to the English field names. The non-external versions are what this API returns to the client. Separating them means the response shape can change independently of what WireMock returns. Lombok generates getters, setters, and `toString()` automatically.

---

## Step 4 — Services

This is where the actual logic lives. Services call WireMock via `RestClient` and map the results.

### `PolicyService.java`

| Method | What it does |
|---|---|
| `getPoliciesByNationalId(nationalId)` | Delegates to `PolicyCacheService` to get all policies for the user |
| `getPolicyById(policyId, nationalId)` | Verifies ownership, then calls `GET /polizas/{policyId}` on WireMock |
| `getConditions(policyId, nationalId)` | Verifies ownership, then calls `GET /polizas/{policyId}/condiciones` on WireMock |
| `getClaims(policyId, nationalId)` | Verifies ownership, then calls `GET /polizas/{policyId}/siniestros` on WireMock |
| `checkOwnership(policyId, nationalId)` | Fetches all the user's policies and checks the requested one belongs to them. Throws `403` if not |

The `toPolicy()` and `toClaim()` private methods map from the `External` DTOs to the internal models.

### `ClaimService.java`

| Method | What it does |
|---|---|
| `getClaimById(claimId)` | Calls `GET /siniestros/{claimId}` on WireMock |

---

## Step 5 — Controllers

Controllers receive HTTP requests and delegate to services. They also extract the authenticated user's identity from `Principal`.

### `PolicyController.java` — base path `/policies`

| Endpoint | What it does |
|---|---|
| `GET /policies` | Gets all policies for the logged-in user |
| `GET /policies/{policyId}` | Gets one policy (ownership verified) |
| `GET /policies/{policyId}/conditions` | Gets the policy's conditions (ownership verified) |
| `GET /policies/{policyId}/claims` | Gets claims linked to a policy (ownership verified) |

`Principal` is injected by Spring Security automatically — it represents the logged-in user. `principal.getName()` returns the username, which in this case is the national ID.

### `ClaimController.java` — base path `/claims`

| Endpoint | What it does |
|---|---|
| `GET /claims/{claimId}` | Gets a single claim by ID |

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

## Step 7 — Caching

### Why caching is needed

Every request that requires ownership verification calls `getPoliciesByNationalId` to fetch all policies for the authenticated user. Without caching, this results in a WireMock call on every single request — even when the data hasn't changed.

### How it works

Spring Boot's built-in caching is enabled with `@EnableCaching` on `InsuranceApplication`. The `@Cacheable` annotation on a method stores the result the first time it is called. On subsequent calls with the same arguments, the stored result is returned directly without calling WireMock.

### `PolicyCacheService.java`

The cached logic lives in a dedicated service rather than in `PolicyService`. This is necessary because Spring caching works through proxies — if a method calls another method within the same class, the proxy is bypassed and the cache is skipped. By extracting `getPoliciesByNationalId` into its own class, every call goes through the proxy correctly.

| Method | Cache name | Cache key |
|---|---|---|
| `getPoliciesByNationalId(nationalId)` | `policies` | `nationalId` |

`PolicyService` delegates to `PolicyCacheService` for both the controller-facing `getPoliciesByNationalId` and the internal `checkOwnership` call, ensuring both benefit from the same cache entry.

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

## Step 8 — Circuit breaking

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

### Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

---

## Request flow

```
Client
  │
  ▼
Spring Security ──── no credentials ──→ 401
  │
  ▼
Controller  (extracts national ID from Principal)
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
3. Make authenticated requests using HTTP Basic Auth

```bash
# Get all policies for the logged-in user
curl -u 12345678A:password http://localhost:8080/policies

# Get a specific policy
curl -u 12345678A:password http://localhost:8080/policies/{policyId}

# Get policy conditions
curl -u 12345678A:password http://localhost:8080/policies/{policyId}/conditions

# Get claims for a policy
curl -u 12345678A:password http://localhost:8080/policies/{policyId}/claims

# Get a specific claim
curl -u 12345678A:password http://localhost:8080/claims/{claimId}
```
