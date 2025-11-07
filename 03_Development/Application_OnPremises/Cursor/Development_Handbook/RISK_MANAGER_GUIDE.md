# Risk Manager Guide

## Overview

The Risk Manager enforces ATP_ProdSpec_54 requirements across the trading engine. It safeguards capital by validating budgets, leverage, exposure, and rolling losses before trades execute, and it can halt traders via emergency stops when thresholds are breached.

Core modules:

- `RiskManager` – orchestrates validations, monitoring, emergency stops.
- `RiskModels` – data structures for risk configuration, scoring, and violations.
- `StopLossManager` – evaluates position and trader stop-loss triggers using rolling 24h P&L.
- `RiskPositionProvider` – lightweight interface implemented by `PositionManager` to supply live positions and history.

## Architecture

```
AITraderManager ─┐                   ┌─ StopLossManager
                 │                   │
                 │   RiskConfig      │
PositionManager ─┼─> RiskManager ────┼─ EmergencyStop / Monitoring
                 │                   │
                 └─> RiskModels ◄────┘
```

- `RiskManager` optionally attaches to `PositionManager` (when provided as the `RiskPositionProvider`) to access exposures and close positions.
- `AITraderManager` delegates trader creation checks and stop callbacks to `RiskManager` (see `attachRiskManager`).
- Monitoring runs in a coroutine loop (configurable interval) to re-evaluate traders and raise alerts.

## Configuration (`RiskConfig`)

| Field | Description |
|-------|-------------|
| `maxTotalBudget` | Aggregate capital cap across all traders (exposure + leverage). |
| `maxLeveragePerTrader` / `maxTotalLeverage` | Hard limits on leverage ratios per trader and globally. |
| `maxExposurePerTrader` / `maxTotalExposure` | Maximum notional exposure (quantity × price × leverage). |
| `maxDailyLoss` | Rolling 24h realized + unrealized loss limit per trader. |
| `stopLossPercentage` | Default percentage used when no explicit stop-loss supplied. |
| `monitoringIntervalSeconds` | Interval for the risk monitoring loop. |

All numeric inputs are validated (non-negative budgets/exposures, positive leverage bounds, positive monitoring interval).

## Key Operations

- `validateBudget(requiredAmount, traderId, leverage)` – checks capital availability and per-trader exposure.
- `validateLeverage(leverage, traderId)` – ensures leverage thresholds are respected.
- `canOpenPosition(traderId, notionalAmount, leverage)` – composite gate called before `PositionManager.openPosition`; it blocks traders under emergency stop in addition to budget/leverage/exposure violations.
- `checkRiskLimits(traderId)` – returns a `RiskCheckResult` containing violations and a `RiskScore` recommendation (ALLOW/WARN/BLOCK/EMERGENCY_STOP).
- `emergencyStop(traderId?)` – closes positions, stops traders, and prevents further execution until manual review.

`RiskScore` combines budget, leverage, exposure, and rolling P&L components; only losses contribute to the P&L score so profitable runs no longer escalate to `EMERGENCY_STOP`.

## Integration Steps

1. **Position Manager** (`PositionManager`)
   - Instantiate `RiskManager` with the manager as the `RiskPositionProvider`.
   - Call `manager.attachRiskManager(riskManager)` to enable pre-trade checks.
   - `openPosition` now fails fast with `PositionException` when risk validation fails.

2. **Trader Manager** (`AITraderManager`)
   - Pass `RiskManager` to constructor or call `attachRiskManager`.
   - Trader creation uses `validateTraderCreation` to enforce “no money available” and “insufficient funds” scenarios before onboarding.
   - Register stop handlers so emergency stops flow back to trader lifecycle methods.

3. **Monitoring & Alerts**
   - Optionally call `riskManager.startMonitoring()` (auto-started in integration tests via `attachRiskManager`).
   - Monitoring loop enforces stop-losses: it closes individual positions whose thresholds are breached and escalates to `emergencyStop` when a trader exceeds rolling loss limits.
   - Provide stop handlers to surface alerts (log, metrics, notifications).

## Usage Example

```kotlin
val riskConfig = RiskConfig(
    maxTotalBudget = BigDecimal("50000"),
    maxLeveragePerTrader = 5,
    maxTotalLeverage = 10,
    maxExposurePerTrader = BigDecimal("15000"),
    maxTotalExposure = BigDecimal("60000"),
    maxDailyLoss = BigDecimal("2000"),
    stopLossPercentage = 0.05
)

val riskManager = RiskManager(positionManager, riskConfig)
positionManager.attachRiskManager(riskManager)
aiTraderManager.attachRiskManager(riskManager)
```

## Testing Strategy

- `RiskManagerTest` exercises budget/leverage validation, exposure blocking, stop-loss/e-stops, monitoring-triggered emergency stops, and emergency-stop gating for new trades.
- Concurrency and end-to-end coverage: `emergency stop is idempotent under concurrent calls`, `end to end risk flow closes positions and notifies handlers` ensure the emergency stop path remains thread-safe and notifies trader handlers.
- `PositionManagerTest` ensures high notional trades are rejected when risk config is restrictive.
- `AITraderManagerTest` verifies trader creation fails when risk budgets are exhausted.
- Run the full suite via `./gradlew clean test --no-daemon`; individual suites: `./gradlew test --tests "RiskManagerTest"`.

## Operational Notes

- Monitoring uses a coroutine scope; tests inject `TestScope` via the constructor for deterministic control.
- `emergencyStop(null)` performs a global halt, closing all positions and calling any registered global stop handler.
- When integrating with new providers, implement `RiskPositionProvider` to supply active positions, history, and closure semantics.

## Troubleshooting

- **Position blocked unexpectedly**: Inspect returned `RiskViolation` details (budget vs leverage vs exposure) to identify the limiting factor.
- **Emergency stop triggered repeatedly**: Verify rolling P&L limits and reset per-trader daily loss configuration if required.
- **Monitoring loop not firing in tests**: Inject a `TestScope` via the `monitoringScope` constructor parameter and advance virtual time.

## Future Enhancements

- Thread-safety stress tests and concurrency fuzzing.
- Dashboard hooks for risk score telemetry and structured alerting.
- Configurable violation escalation paths (e.g., WARN → BLOCK thresholds per trader class).


