# Test Cases

**Base URL:** `http://localhost:8080`
**WireMock URL:** `http://localhost:8081`
**Auth method:** HTTP Basic Auth

**Test users:**

| Username | Password |
|---|---|
| `12345678A` | `password` |
| `87654321B` | `password` |

---

## 1. Authentication

### TC-01 — No credentials

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/policies
```

**Expected:** `401 Unauthorized`
**Result:** `401` ✅

---

## 2. Policies

### TC-02 — Get all policies for authenticated user

```bash
curl -s -u 12345678A:password http://localhost:8080/policies
```

**Expected:** `200` with list of policies belonging to `12345678A`
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

### TC-03 — Get policy detail (own policy)

```bash
curl -s -u 12345678A:password http://localhost:8080/policies/1234512345678A
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

### TC-04 — Get policy detail (another user's policy)

```bash
curl -s -u 87654321B:password http://localhost:8080/policies/1234512345678A
```

**Expected:** `403 Forbidden` — user `87654321B` does not own this policy
**Result:** `403` ✅

```json
{
  "error": "Access denied to policy 1234512345678A"
}
```

---

### TC-05 — Get policy conditions (own policy)

```bash
curl -s -u 12345678A:password http://localhost:8080/policies/1234512345678A/conditions
```

**Expected:** `200` with list of conditions
**Result:** `200` ✅

```json
["Condición A", "Condición B", "Condición C"]
```

---

### TC-06 — Get policy conditions (another user's policy)

```bash
curl -s -u 87654321B:password http://localhost:8080/policies/1234512345678A/conditions
```

**Expected:** `403 Forbidden`
**Result:** `403` ✅

```json
{
  "error": "Access denied to policy 1234512345678A"
}
```

---

### TC-07 — Get claims linked to a policy

```bash
curl -s -u 12345678A:password http://localhost:8080/policies/1234512345678A/claims
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

> **Note:** `description` and `date` are `null` because the WireMock stub for `/polizas/{policyId}/siniestros` only returns `siniestroId` and `estado`. Full claim detail (including `description` and `date`) is available via `GET /claims/{claimId}`.

---

## 3. Claims

### TC-08 — Get claim detail by ID

```bash
curl -s -u 12345678A:password http://localhost:8080/claims/A1234512345678A
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

## 4. User isolation

### TC-09 — Each user only sees their own policies

```bash
curl -s -u 87654321B:password http://localhost:8080/policies
```

**Expected:** `200` with policies belonging to `87654321B` only (different from `12345678A`)
**Result:** `200` ✅

```json
[
  {
    "policyId": "1234587654321B",
    "description": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    "coverages": null
  }
]
```

---

## 5. Circuit breaker

### TC-10 — WireMock unavailable triggers fallback with 502

WireMock stopped, then:

```bash
curl -s -u 12345678A:password http://localhost:8080/policies
```

**Expected:** `502 Bad Gateway` with error message
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
| TC-01 | No credentials | `401` | ✅ |
| TC-02 | Get own policies | `200` | ✅ |
| TC-03 | Get own policy detail | `200` | ✅ |
| TC-04 | Get another user's policy detail | `403` | ✅ |
| TC-05 | Get own policy conditions | `200` | ✅ |
| TC-06 | Get another user's policy conditions | `403` | ✅ |
| TC-07 | Get claims for own policy | `200` | ✅ |
| TC-08 | Get claim detail by ID | `200` | ✅ |
| TC-09 | User isolation (each user sees own data) | `200` | ✅ |
| TC-10 | Circuit breaker fires when WireMock is down | `502` | ✅ |
