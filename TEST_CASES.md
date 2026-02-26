# Test Cases

**Base URL:** `http://localhost:8080`
**WireMock URL:** `http://localhost:8081`
**Auth method:** JWT Bearer token (self-issued via `/auth/login`)

**How to get a token:**

```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}' | jq -r '.accessToken')
```

> **Note on user identity:** `principal.getName()` returns the username passed to `/auth/login` (e.g. `senkroh`). The ownership check in `PolicyService` uses this username to query WireMock.

---

## 1. Authentication

### TC-00 — Login and obtain JWT token

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}'
```

**Expected:** `200` with a signed access token and a refresh token
**Result:** `200` ✅

```json
{
  "accessToken": "<jwt_string>",
  "refreshToken": "<uuid_string>"
}
```

---

### TC-01 — No token

```bash
curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/policies"
```

**Expected:** `401 Unauthorized`
**Result:** `401` ✅

---

### TC-02 — Invalid token

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer invalid.token.here" \
  "http://localhost:8080/policies"
```

**Expected:** `401 Unauthorized` — signature validation fails
**Result:** `401` ✅

---

### TC-03 — Get current user

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/user"
```

**Expected:** `200` with the authenticated username
**Result:** `200` ✅

```json
{
  "name": "senkroh"
}
```

---

### TC-11 — Expired token is rejected

Decode the token at [jwt.io](https://jwt.io) and wait for the `exp` timestamp to pass (1 hour), then:

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/user"
```

**Expected:** `401 Unauthorized` — token expired
**Result:** `401` ✅

---

### TC-12 — Logout blacklists the token server-side

```bash
# 1. Login and capture both tokens
export TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}' | jq -r '.accessToken')

export REFRESH=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}' | jq -r '.refreshToken')

# 2. Logout
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

**Expected:** `204 No Content`
**Result:** `204` ✅

```bash
# 3. Try the old access token — should now be rejected
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/user"
```

**Expected:** `401 Unauthorized` — token is blacklisted server-side
**Result:** `401` ✅

---

### TC-14 — Refresh token issues a new access token

```bash
# 1. Login
RESPONSE=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"senkroh"}')

export REFRESH=$(echo $RESPONSE | jq -r '.refreshToken')

# 2. Use refresh token to get a new access token
curl -s -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

**Expected:** `200` with a new access token
**Result:** `200` ✅

```json
{
  "accessToken": "<new_jwt_string>"
}
```

---

### TC-15 — Rate limiter blocks excessive login attempts

```bash
for i in {1..6}; do
  curl -s -o /dev/null -w "Request $i: %{http_code}\n" \
    -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"senkroh"}'
done
```

**Expected:** First 5 requests return `200`, 6th returns `429 Too Many Requests`
**Result:** `429` ✅

```json
{
  "error": "Too many requests, please try again"
}
```

---

## 2. Policies

### TC-04 — Get all policies for authenticated user

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies"
```

**Expected:** `200` with list of policies belonging to the authenticated user
**Result:** `200` ✅

```json
[
  {
    "policyId": "1234512345678A",
    "description": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    "coverages": null
  }
]
```

---

### TC-05 — Get policy detail (own policy)

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies/1234512345678A"
```

**Expected:** `200` with full policy detail
**Result:** `200` ✅

```json
{
  "policyId": "1234512345678A",
  "description": "Detalle de la póliza con id_poliza: 1234512345678A",
  "coverages": ["Cobertura A", "Cobertura B"]
}
```

---

### TC-06 — Get policy detail (policy not owned by user)

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies/1234587654321B"
```

**Expected:** `403 Forbidden` — policy does not belong to the authenticated user
**Result:** `403` ✅

```json
{
  "error": "Access denied to policy 1234587654321B"
}
```

---

### TC-07 — Get policy conditions (own policy)

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies/1234512345678A/conditions"
```

**Expected:** `200` with list of conditions
**Result:** `200` ✅

```json
["Condición A", "Condición B", "Condición C"]
```

---

### TC-08 — Get policy conditions (policy not owned by user)

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies/1234587654321B/conditions"
```

**Expected:** `403 Forbidden`
**Result:** `403` ✅

```json
{
  "error": "Access denied to policy 1234587654321B"
}
```

---

### TC-09 — Get claims linked to a policy

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies/1234512345678A/claims"
```

**Expected:** `200` with list of claims
**Result:** `200` ✅

```json
[
  {
    "claimId": "A1234512345678A",
    "status": "En proceso",
    "description": null,
    "date": null
  },
  {
    "claimId": "B1234512345678A",
    "status": "En proceso",
    "description": null,
    "date": null
  }
]
```

> **Note:** `description` and `date` are `null` because the WireMock stub for `/polizas/{policyId}/siniestros` only returns `siniestroId` and `estado`. Full claim detail is available via `GET /claims/{claimId}`.

---

## 3. Claims

### TC-10 — Get claim detail by ID

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/claims/A1234512345678A"
```

**Expected:** `200` with full claim detail
**Result:** `200` ✅

```json
{
  "claimId": "A1234512345678A",
  "description": "Detalle del siniestro con id_siniestro: A1234512345678A",
  "status": "En proceso",
  "date": "2024-08-02"
}
```

---

## 4. Resilience

### TC-13 — WireMock unavailable triggers fallback with 502

Stop WireMock, then:

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/policies"
```

**Expected:** `502 Bad Gateway`
**Result:** `502` ✅

```json
{
  "error": "Error fetching policies"
}
```

---

## Summary

| ID | Scenario | HTTP Status | Result |
|---|---|---|---|
| TC-00 | Login — returns accessToken + refreshToken | `200` | ✅ |
| TC-01 | No token | `401` | ✅ |
| TC-02 | Invalid token | `401` | ✅ |
| TC-03 | Get current user | `200` | ✅ |
| TC-04 | Get own policies | `200` | ✅ |
| TC-05 | Get own policy detail | `200` | ✅ |
| TC-06 | Get another user's policy detail | `403` | ✅ |
| TC-07 | Get own policy conditions | `200` | ✅ |
| TC-08 | Get another user's policy conditions | `403` | ✅ |
| TC-09 | Get claims for own policy | `200` | ✅ |
| TC-10 | Get claim detail by ID | `200` | ✅ |
| TC-11 | Expired token rejected | `401` | ✅ |
| TC-12 | Logout blacklists token server-side | `204` + `401` | ✅ |
| TC-13 | Circuit breaker fires when WireMock is down | `502` | ✅ |
| TC-14 | Refresh token issues new access token | `200` | ✅ |
| TC-15 | Rate limiter blocks excessive login attempts | `429` | ✅ |
