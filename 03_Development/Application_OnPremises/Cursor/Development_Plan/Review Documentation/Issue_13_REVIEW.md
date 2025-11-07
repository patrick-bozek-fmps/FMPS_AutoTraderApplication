# Issue #13: Position Manager - Task Review & QA Report

**Review Date**: November 7, 2025  
**Reviewer**: Software Engineer - Task Review and QA  
**Issue Status**: COMPLETE (per issue doc)  
**Review Status**: FAIL

---

## Summary

Issue #13 delivered the initial `PositionManager`, persistence bridge, and companion tests/documentation. The core lifecycle (open/update/close/recover/monitor) and persistence helpers are present, and the Position Manager guide aligns with the implementation in broad strokes. However, several critical gaps remain:

- Closing a position continues even when the database update fails, which can leave trades stranded as "OPEN" while the in-memory manager removes them.  
```332:345:core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt
                if (managedPosition.tradeId != null) {
                    val closedTrade = positionPersistence.closePosition(
                        tradeId = managedPosition.tradeId,
                        exitPrice = exitPrice,
                        exitAmount = placedOrder.filledQuantity,
                        exitReason = normalizedReason,
                        exitOrderId = placedOrder.id
                    )

                    if (closedTrade != null && closedTrade.profitLoss != null) {
                        realizedPnL = closedTrade.profitLoss
                    }
                }
```
- Trailing stop support is claimed in the plan but never implemented—`updateStopLoss` forwards the trailing flag but discards it, and `ManagedPosition` has no notion of a trailing stop.  
```465:485:core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt
    suspend fun updateStopLoss(
        positionId: String,
        newStopLoss: BigDecimal,
        trailingActivated: Boolean = false
    ): Result<Unit> {
        return positionsMutex.withLock {
            val managedPosition = activePositions[positionId]
                ?: return Result.failure(IllegalArgumentException("Position not found: $positionId"))

            if (managedPosition.tradeId != null) {
                val updated = positionPersistence.updateStopLoss(managedPosition.tradeId, newStopLoss, trailingActivated)
                if (!updated) {
                    return Result.failure(PositionException("Failed to update stop-loss in repository"))
                }
            }

            activePositions[positionId] = managedPosition.copy(stopLossPrice = newStopLoss)
            logger.info { "Updated stop-loss for position $positionId to $newStopLoss" }
            Result.success(Unit)
        }
    }
```
  Documentation still asserts trailing-stop persistence is complete.  
```65:67:03_Development/Application_OnPremises/Cursor/Development_Plan/Issue_13_Position_Manager.md
- [x] Added `updateStopLoss` persisting trailing flag and value via repository
- [x] Monitoring coroutine auto-closes positions when stop-loss threshold reached
- [x] Unit tests cover detection, persistence sync, monitoring auto-close
```
- Planning artifacts have not been synchronised: the epic status page still contains sections saying "Ready to start Issue #13" and the issue plan keeps Task 12 (commit/push/CI) pending.  
```92:102:03_Development/Application_OnPremises/Cursor/Development_Plan/EPIC_3_STATUS.md
- No issues currently in progress - Ready to start Issue #13
...
| Issue #13: Position Manager | ✅ COMPLETE |
```
```115:121:03_Development/Application_OnPremises/Cursor/Development_Plan/Issue_13_Position_Manager.md
### **Task 12: Build & Commit** [Status: ⏳ PENDING]
- [x] Run full build: `./gradlew build --no-daemon`
- [x] Fix any compilation/test issues (PositionManagerTest flake adjusted)
- [ ] Commit documentation + code changes
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update final commit hash above
```
- Re-running the documented test command now fails because the suite expects Risk Manager-driven emergency stops that are not yet implemented in PositionManager.  
```
> .\gradlew.bat :core-service:test --tests "*PositionManagerTest*"
...
PositionManagerTest > Emergency stop closes open positions FAILED
    org.opentest4j.AssertionFailedError: Remaining positions: [...] ==> expected: <true> but was: <false>
```

---

## Findings

| Severity | Area | Description & Evidence |
|----------|------|-------------------------|
| ❗ **Critical** | Persistence | `closePosition` removes positions from memory even when `PositionPersistence.closePosition` fails, leaving the trade open in the database and recovery logic will re-surface the supposedly closed position.  
```332:374:core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt
                if (managedPosition.tradeId != null) {
                    val closedTrade = positionPersistence.closePosition(...)
                    if (closedTrade != null && closedTrade.profitLoss != null) {
                        realizedPnL = closedTrade.profitLoss
                    }
                }
                ...
                activePositions.remove(positionId)
```
| ❗ **Major** | Stop-Loss / Trailing | The plan marks trailing stop persistence as complete, yet `ManagedPosition` has no trailing metadata and `updateStopLoss` only overwrites the static price. Monitoring therefore cannot honour trailing stops.  
```465:485:core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt
activePositions[positionId] = managedPosition.copy(stopLossPrice = newStopLoss)
```
| ⚠️ **Moderate** | Documentation | Project status docs still contain contradictory statements (“ready to start Issue #13”) and the issue plan leaves Task 12 (commit/push/CI) unchecked even though the code is in the repo.  
```92:102:03_Development/Application_OnPremises/Cursor/Development_Plan/EPIC_3_STATUS.md
- No issues currently in progress - Ready to start Issue #13
```
| ⚠️ **Moderate** | Testing | `./gradlew :core-service:test --tests "*PositionManagerTest*"` now fails (`Emergency stop closes open positions`) because the suite expects Risk Manager integration that PositionManager has not implemented. This blocks re-validation of Issue 13 behaviour until the risk work stabilises. |

---

## Verification

- `.\gradlew.bat :core-service:test --tests "*PositionManagerTest*"` *(fails on “Emergency stop closes open positions”)*

## Observations

- The Position Manager successfully covers opening, updating, closing, persistence bridging, history queries, recovery, and monitoring loops as originally specified.
- Risk Manager hooks were not part of Issue 13, but new tests now target that integration; until the risk work is merged, re-running the Position Manager suite will continue to fail.
- Documentation should be updated once the outstanding tasks (commit hash, CI confirmation, epic status) are resolved.

## Recommended Actions

1. Treat repository update failures as fatal inside `closePosition`—surface the error instead of removing the in-memory position.
2. Either implement trailing stop behaviour (store flag, update logic) or downgrade the documentation claims until the feature exists.
3. Synchronise planning/stats documents with reality (mark Issue 13 sections as complete, update Task 12 checklist, add final commit hash/CI evidence).
4. Coordinate with the Risk Manager work so that Position Manager tests either stub emergency stops or defer the new expectations until the implementation lands.
