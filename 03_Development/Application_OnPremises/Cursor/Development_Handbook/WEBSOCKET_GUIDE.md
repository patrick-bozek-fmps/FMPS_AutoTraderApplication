 # WebSocket Telemetry Guide

 **Version**: 1.0  
 **Last Updated**: November 11, 2025  
 **Status**: Active  

 ---

 ## 🎯 Overview

 The WebSocket telemetry endpoint delivers real-time updates for the desktop UI, automation clients, and operational tooling.  
 The gateway runs at `/ws/telemetry` and shares the same API key authentication model as the REST surface.

 ---

 ## 🔐 Authentication

 - Header: `X-API-Key` (configurable via `security.api` block)  
 - Query fallback: `apiKey`  
 - Requests without a valid key receive `4401` (`VIOLATED_POLICY`) close reason.  
 - Auth is enforced for WebSocket handshakes and all admin REST helpers.

 ---

 ## 📡 Channels

 | Channel | Wire Name | Description | Replay Support |
 |---------|-----------|-------------|----------------|
 | Trader Status | `trader-status` | Lifecycle updates for each AI trader (status, exchange, strategy, metrics – uptime encoded as ISO-8601 duration) | ✅ |
 | Positions | `positions` | Position lifecycle events with P&L, quantity, trailing stop info | ✅ |
 | Risk Alerts | `risk-alerts` | Critical and warning level risk violations with recommendations | ✅ |
 | Market Data | `market-data` | Latest sampled price snapshots per symbol (per exchange source) | ✅ |

 Replay requests stream the latest cached snapshots (bounded by `telemetry.replayLimit`, default 50 events).

 ---

 ## 🔄 Client Protocol

 ### Connection Parameters

 ```
 /ws/telemetry?clientId={custom-id}&channels=trader-status,risk-alerts&replay=true
 ```

 - `clientId` (optional): deterministic identifier for admin management (UUID auto-generated if omitted)
 - `channels` (optional): comma-delimited list of channels to auto-subscribe on connect
 - `replay` (optional): `true` replays cached snapshots after subscription acknowledgement

 ### Server → Client Messages

 | Type | Description |
 |------|-------------|
 | `welcome` | Connection accepted, includes negotiated heartbeat interval and rate limit |
 | `ack` | Subscription or unsubscribe acknowledgement |
 | `event` | Domain event payload (`channel` + `data`) |
 | `heartbeat` | Server heartbeat tick (also used as ACK reply) |
 | `error` | Recoverable issue (invalid payload, unknown action, auth failure) |

 ### Client → Server Actions

 ```json
 {"action":"subscribe","channels":["trader-status","positions"],"replay":true}
 {"action":"unsubscribe","channels":["risk-alerts"]}
 {"action":"heartbeat"}
 ```

 ### Heartbeats

 - Server tick interval: configurable via `telemetry.heartbeatIntervalSeconds` (default 15s)
 - Timeout: disconnects after `telemetry.heartbeatTimeoutSeconds`
 - Clients should respond with `{"action":"heartbeat"}` or send any control frame within the window

 ### Rate Limiting

 - Soft limit per connection controlled by `telemetry.rateLimitPerSecond` (default 120 messages/sec)
 - Messages beyond the limit are dropped and counted in telemetry metrics (`messagesDropped` totals)

 ---

 ## 📊 Metrics & Observability

 Metrics are exported through Prometheus (`/metrics`) and include:

 - `autotrader.telemetry.active_connections`
 - `autotrader.telemetry.messages.sent.total`
 - `autotrader.telemetry.messages.dropped.total`
 - Per-channel gauges and counters (`autotrader.telemetry.messages.sent.channel{channel="trader-status"}`, etc.)

 Structured logging is emitted for:

 - Connection accepted/closed
 - Subscriptions and unsubscriptions
 - Forced disconnects and heartbeat timeouts
 - Message dispatch failures

 ---

 ## 🛠️ Administration Endpoints

 | Endpoint | Method | Description |
 |----------|--------|-------------|
 | `/api/v1/websocket/stats` | GET | Aggregate metrics + snapshot counts |
 | `/api/v1/websocket/clients` | GET | Active client list (ID, remote address, channels, last heartbeat) |
 | `/api/v1/websocket/clients/{clientId}` | DELETE | Forcefully disconnect a client (supports optional `reason` query param) |

 All endpoints require an API key unless excluded via `security.api.excludedPaths`.

 ---

 ## ✅ Client Integration Checklist

 - [ ] Provide API key header on handshake  
 - [ ] Handle `welcome` message and capture heartbeat interval  
 - [ ] Subscribe to desired channels using `subscribe` action (or handshake query parameters)  
 - [ ] Implement heartbeat keep-alive (respond to server pings or send `{"action":"heartbeat"}`)  
 - [ ] Honour `replay` flag if cached data is required  
 - [ ] Back off or resubscribe if `error` messages reference invalid actions  
 - [ ] Monitor admin endpoints or Prometheus metrics for operational visibility  

 ---

 ## 🔧 Configuration Reference (`application.conf`)

 ```hocon
 telemetry {
   heartbeatIntervalSeconds = 15
   heartbeatTimeoutSeconds = 45
   rateLimitPerSecond = 120
   replayLimit = 50
 }
 ```

 ---

 ## 🧪 Testing Tips

 - Use `wscat` or Postman to validate manual flows:
   ```
   wscat -H "X-API-Key: dev-api-key" ws://localhost:8080/ws/telemetry
   ```
 - Automated coverage lives in `TelemetryRouteTest` (authentication, replay, admin API)
 - For load/regression, monitor Prometheus counters during replay + live publish loops

 ---

 ## 📚 Related Documents

 - `Issue_17_WebSocket_Telemetry.md`
 - `EPIC_4_STATUS.md`
 - `Development_Plan_v2.md`
 - `API_REFERENCE.md`
 - `CONFIG_GUIDE.md`


