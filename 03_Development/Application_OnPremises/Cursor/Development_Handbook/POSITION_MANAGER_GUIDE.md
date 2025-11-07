# Position Manager Guide

**Version**: 1.0  
**Last Updated**: November 7, 2025  
**Module**: `core-service/traders`  
**Issue**: #13 – Position Manager

---

## 📋 Overview

`PositionManager` orchestrates the full lifecycle of trading positions for the AI Trading Engine:

- Opens positions from actionable trading signals
- Keeps positions synchronized with live exchange data
- Executes stop-loss and take-profit logic automatically
- Persists all trades to the database and recovers them after restart
- Maintains a consolidated history view with aggregate metrics

The implementation is intentionally connector-agnostic. All exchange interactions go through `IExchangeConnector`; persistence is centralized in `PositionPersistence`, which wraps the Exposed-based `TradeRepository`.

---

## 🏗 Architecture

```
AI Trader
   │
   ▼
PositionManager ──────┐
  │                   │
  │ uses              │
  ▼                   ▼
IExchangeConnector   PositionPersistence ──► TradeRepository (Exposed ORM)
  │                   │
  │ provides          ▼
  └─► Ticker / Orders / Positions   PositionHistory + Metrics
```

### Core classes

- `PositionManager`
  - Thread-safe manager responsible for active positions, monitoring and aggregates
  - Holds `activePositions: MutableMap<String, ManagedPosition>` guarded by a `Mutex`
  - Maintains an in-memory `positionHistory` cache for just-closed trades
- `ManagedPosition`
  - Runtime wrapper around the shared `Position` data class, enriched with manager metadata (IDs, stops, timestamp)
- `PositionPersistence`
  - Conversion helpers between `Trade` rows and runtime types
  - Saves/updates/loads trades through `TradeRepository`
- `PositionHistory`
  - Immutable record for completed positions used by reporting APIs

---

## 🔄 Lifecycle & APIs

### Opening a position

```kotlin
val result = positionManager.openPosition(
    signal = signal,
    traderId = "42",
    symbol = "BTCUSDT",
    stopLossPrice = BigDecimal("29500"),
    takeProfitPrice = BigDecimal("31500")
)
```

Steps executed:
1. Validate that the `TradingSignal` is actionable (BUY/SELL) and meets confidence requirements
2. Derive the trade action (`LONG`/`SHORT`) and fetch the latest price via `getTicker(symbol)`
3. Compute default position size using signal confidence with price-based scaling (configurable via parameter override)
4. Submit a market order through the connector. Partial/failed fills bubble up as `PositionException`
5. Build `ManagedPosition` + persist trade metadata through `PositionPersistence.savePosition`
6. Cache position in-memory and return to caller

### Updating

- `updatePosition(positionId, currentPrice?)` fetches ticker data when the price is not provided, recalculates P&L, and refreshes timestamps
- `refreshPosition(positionId)` pulls the authoritative exchange snapshot (quantity, entry price, current price) and reconciles the managed position

### Closing

```kotlin
positionManager.closePosition(positionId, reason = "MANUAL")
```

- Places an opposing market order, calculates realized P&L, writes closure data to the repository, and appends a `PositionHistory` entry
- Reasons are normalized to uppercase (STOP_LOSS, TAKE_PROFIT, MANUAL, SIGNAL, ERROR, ORPHANED)

### Monitoring & Auto-Execution

```kotlin
positionManager.startMonitoring()
// ...later
positionManager.stopMonitoring()
```

The monitoring coroutine runs on `Dispatchers.Default` with a configurable interval (default 5s). Each cycle:

1. Takes a snapshot of active position IDs
2. Calls `updatePosition` to refresh prices/P&L
3. Triggers stop-loss / take-profit closures when thresholds are hit
4. Logs outcomes and continues even if one position throws

Take-profit and stop-loss levels can be adjusted at runtime:

```kotlin
positionManager.updateStopLoss(positionId, newStop, trailingActivated = true)
positionManager.updateTakeProfit(positionId, newTarget)
```

Both methods synchronize in-memory state and persist changes through `PositionPersistence`.

### Recovery

```kotlin
positionManager.recoverPositions()
```

- Loads all open trades from `TradeRepository`
- For each trade, verifies the matching exchange position
  - If present: rebuilds a `ManagedPosition` with recalculated P&L
  - If missing: marks the trade as `ORPHANED` and closes it in the database
- Logs failures but continues processing the remainder

---

## 📊 History & Metrics

APIs combine in-memory and persisted history, deduplicated per trade ID:

```kotlin
val traderHistory = positionManager.getHistoryByTrader("42")
val symbolHistory = positionManager.getHistoryBySymbol("ETHUSDT")
val rangeHistory = positionManager.getHistoryByDateRange(start, end)

val totalPnL = positionManager.getTotalPnL()
val winRate = positionManager.getWinRate()
```

`TradeRepository` utilities added for:
- Closed trades by trader, symbol, date range
- Stop-loss / take-profit updates
- Orphaned trade closure handling

---

## ✅ Testing

| Scenario | Coverage |
|----------|----------|
| Position opening | BUY, SELL, HOLD rejection |
| Updates & P&L | Price changes, LONG/SHORT calculations |
| Persistence sync | Stop-loss / take-profit updates, close propagation |
| Recovery | Restores trades from DB, handles missing exchange positions |
| Monitoring | Auto stop-loss close via coroutine loop |
| History & metrics | Total PnL (zero baseline), win-rate defaults, history retrieval |

Command:
```powershell
./gradlew :core-service:test --tests "*PositionManagerTest*" --no-daemon
```

---

## 🔁 Integration Points

- **AI Trader**: Passes actionable signals, receives managed position handles
- **Risk Manager (Issue #14)**: Consumes history/P&L metrics to assess exposure
- **Pattern Storage (Issue #15)**: Can query realized results for reinforcement signals

---

## 🛠 Troubleshooting

| Symptom | Likely Cause | Resolution |
|---------|--------------|-----------|
| Position not auto-closing on stop | Monitoring not started or interval too high | Ensure `startMonitoring()` is called; adjust `updateInterval` |
| P&L appears zero | Using `getTotalPnL()` before any close recorded | Confirm positions are closed; check database entries |
| Recovery logs "orphaned" trade | Exchange position missing | Verify manual closures, review exit reason `ORPHANED` |
| Decimal precision mismatch | Comparing `BigDecimal` with varying scales | Use `compareTo` or `stripTrailingZeros()` (already applied in tests) |

---

## 📚 References

- `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionPersistence.kt`
- `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/PositionManagerTest.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/database/repositories/TradeRepository.kt`
- `Development_Plan/Issue_13_Position_Manager.md`

---

**Next steps**
- Integrate Risk Manager (#14) to apply leverage/budget checks before opening positions
- Add optional trailing stop-loss and time-based exits in future iterations
- Extend monitoring to broadcast telemetry metrics (planned for Epic 6)
