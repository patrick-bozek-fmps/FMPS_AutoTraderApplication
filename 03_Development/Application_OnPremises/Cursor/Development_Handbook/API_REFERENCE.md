# FMPS AutoTrader Core API Reference

**Version**: 1.0  
**Updated**: November 11, 2025  
**Maintainer**: AI Assistant  

---

## 1. Overview

The Core service exposes a REST and metrics surface under `/api/v1` (REST) and `/metrics` (Prometheus). Issue #16 (Epic 4) introduced mandatory API key authentication, standardised pagination/validation, and Prometheus-ready observability. This reference captures the conventions needed by the desktop UI (Epic 5) and operational tooling (Epic 6).

All examples assume the service is running locally on `http://localhost:8080` with the default development API key `dev-api-key`.

---

## 2. Authentication & Security

### 2.1 API Key Requirements

- Every non-health request **must** supply a valid API key.
- Supported mechanisms:
  - HTTP header: `X-API-Key: <key>`
  - Query parameter (fallback for legacy scripts): `?apiKey=<key>`
- Unauthorized requests respond with `401` and an `ErrorResponse` body.

### 2.2 Configuration

The security block in `application.conf` (with overrides in `application-*.conf` or environment variables) controls API key behaviour.

```hocon
security {
  api {
    enabled = true
    header = "X-API-Key"
    queryParam = "apiKey"

    # Provide either `key` (single) or `keys` (list). Both may be overridden by env vars.
    key = "dev-api-key"
    keys = ["dev-api-key"]       # optional list form
    envKey = "FMPS_API_KEY"      # optional override from environment

    excludedPaths = [
      "/api/health",
      "/api/status",
      "/api/version"
    ]
  }
}
```

> **Note**: The `/metrics` endpoint is protected by default. Remove it from `excludedPaths` only after thorough review.

### 2.3 Error Envelope

Unauthorized or invalid requests reuse the shared `ErrorResponse` format:

```json
{
  "success": false,
  "error": {
    "code": "API_KEY_INVALID",
    "message": "Provided API key is not valid.",
    "details": {
      "path": "/api/v1/trades",
      "header": "X-API-Key",
      "queryParam": "apiKey"
    }
  },
  "timestamp": "2025-11-11T10:15:30.123Z"
}
```

---

## 3. REST Endpoints

### 3.1 Health & Status

| Method | Path            | Auth Required | Description                                    |
|--------|-----------------|---------------|------------------------------------------------|
| GET    | `/api/health`   | No            | Lightweight uptime probe with uptime counter. |
| GET    | `/api/status`   | No            | Includes active trader count and DB summary.   |
| GET    | `/api/version`  | No            | Returns API and build version metadata.        |

### 3.2 Trades

`GET /api/v1/trades` now supports standard pagination and filters:

| Parameter    | Type    | Required | Default | Notes                                                                 |
|--------------|---------|----------|---------|-----------------------------------------------------------------------|
| `page`       | int     | No       | `1`     | Must be ≥ 1.                                                          |
| `pageSize`   | int     | No       | `50`    | 1 – 200. Legacy `limit` parameter maps to `pageSize`.                 |
| `status`     | string  | No       | –       | `OPEN` or `CLOSED`. Case-insensitive.                                 |
| `aiTraderId` | int     | No       | –       | Filters trades belonging to the specified trader.                     |

Successful responses use `PaginatedResponse`:

```json
{
  "success": true,
  "data": [ /* list of TradeDTO */ ],
  "pagination": {
    "page": 2,
    "pageSize": 50,
    "totalItems": 178,
    "totalPages": 4
  },
  "timestamp": "2025-11-11T10:20:00.000Z"
}
```

Validation issues propagate descriptive error codes such as `INVALID_PAGE`, `INVALID_PAGE_SIZE`, or `INVALID_STATUS`.

Additional trade routes (`/api/v1/trades/open`, `/api/v1/trades/{id}`, etc.) honour the same auth requirements.

### 3.3 Configuration & Patterns

Configuration and pattern routes remain stubs in v1.0 but are now secured. Expect `404`/`501` placeholder responses until Issues #17–#18 expand functionality.

---

## 4. Metrics & Observability

### 4.1 Prometheus Endpoint

- Path: `GET /metrics`
- Requires API key (unless explicitly excluded in configuration).
- Exposes JVM and system metrics via Micrometer binders:
  - `jvm_memory_used_bytes`, `jvm_threads_live`, `process_cpu_usage`, etc.
- Custom request timers carry automatic tags (`uri`, `status`, `method`).

Example scrape:

```bash
curl -H "X-API-Key: dev-api-key" http://localhost:8080/metrics
```

### 4.2 Logging

Existing `CallLogging` remains active; unauthorized attempts emit warn-level entries (`Unauthorized request to /api/v1/...`). No additional action is required—ensure log retention per the logging guide.

---

## 5. Testing & Verification

- Run the full suite: `./gradlew clean test --no-daemon`
- New coverage:
  - `ApiSecurityTest` validates missing/invalid/valid API key flows and metrics protection.
  - `TradeRepositoryTest` exercises paginated queries (page, pageSize, status filters).
- Manual smoke:
  - `curl -H "X-API-Key: dev-api-key" http://localhost:8080/api/v1/trades?page=1&pageSize=10`
  - `curl http://localhost:8080/api/health` (unauthenticated)
  - `curl -H "X-API-Key: dev-api-key" http://localhost:8080/metrics`

---

## 6. Integration Checklist

1. **Configure API Key**
   - Production: set `FMPS_API_KEY` or populate `security.api.keys` in `application-prod.conf`.
   - Development: default `dev-api-key` works out of the box.
2. **Desktop UI Consumption (Epic 5)**
   - Add `X-API-Key` header to all REST and WebSocket requests.
   - Adopt pagination metadata to drive infinite scroll / paging components.
3. **Observability**
   - Register `/metrics` with Prometheus (or scrape manually).
   - Ensure API key secrecy by restricting metrics dashboard visibility.
4. **CI/CD**
   - Pipeline step should export `FMPS_API_KEY` when running integration smoke tests.

---

## 7. Known Limitations & Roadmap

- OAuth2 / RBAC is deferred beyond v1.0 (Epic 6+).
- Metrics endpoint currently returns global JVM/system stats; API-level counters will be enriched in Issue #17.
- `ConfigRoutes`/`PatternRoutes` responses are placeholders until relevant issues land.

---

## 8. References

- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/plugins/Security.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/plugins/Monitoring.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/TradeRoutes.kt`
- `core-service/src/test/kotlin/com/fmps/autotrader/core/api/ApiSecurityTest.kt`
- `core-service/src/test/kotlin/com/fmps/autotrader/core/database/repositories/TradeRepositoryTest.kt`
- Epic tracking: `Cursor/Development_Plan/Issue_16_Core_Service_API.md`, `EPIC_4_STATUS.md`

---

For questions or suggested improvements, open an entry in `Issue_16_Core_Service_API.md` (Notes & Learnings) or contact the project owner.

