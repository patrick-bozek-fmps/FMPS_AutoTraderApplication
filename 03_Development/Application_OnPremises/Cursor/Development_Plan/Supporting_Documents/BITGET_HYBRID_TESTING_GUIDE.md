# Bitget Hybrid Testing Guide

**Status**: ✅ Implemented  
**Date**: November 20, 2025  
**Related Defect**: [DEF_009](../Defects/DEF_009_Bitget_Connector_Integration_Test_Failures.md)

---

## 📋 Overview

This document describes the hybrid testing approach implemented for Bitget connector integration tests, which automatically discovers and uses API-version-compatible symbols for testing.

---

## 🔍 Problem Statement

**Challenge**: 
- Bitget V1 and V2 APIs have different symbol lists
- V1 market endpoints don't support `BTCUSDT` (confirmed by V2)
- Hardcoding `BTCUSDT` in tests causes failures
- Need adaptive tests that work with available symbols

**Solution**: Hybrid testing that:
- Discovers V1-compatible symbols automatically
- Discovers V2-compatible symbols automatically
- Uses appropriate symbols for each API version
- Falls back gracefully if no compatible symbols found

---

## 💡 Hybrid Testing Strategy

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│         BitgetConnector (onConnect)                      │
│                                                         │
│  1. Query V2 symbols endpoint                          │
│     → Get all V2-compatible symbols (790 pairs)         │
│                                                         │
│  2. Test V2 symbols with V1 endpoints                 │
│     → Discover V1-compatible symbols                   │
│                                                         │
│  3. Store discovered symbols:                           │
│     - v1CompatibleSymbols: List<String>                │
│     - v2CompatibleSymbols: List<String>                 │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│      BitgetConnectorIntegrationTest                      │
│                                                         │
│  @Order(2) test connection()                           │
│    → Connects and discovers symbols                    │
│    → Stores: v1TestSymbol, v2TestSymbol                │
│                                                         │
│  @Order(3+) Market endpoint tests                      │
│    → Use v1TestSymbol for V1 tests                     │
│    → Use v2TestSymbol for V2 tests (when available)   │
│    → Fallback to BTCUSDT if none found                │
└─────────────────────────────────────────────────────────┘
```

### How It Works

#### 1. Symbol Discovery (During Connection)

**Location**: `BitgetConnector.onConnect()`

**Process**:
1. **Query V2 Symbols Endpoint**: Get all available symbols from `/api/v2/spot/public/symbols`
   - Filters for symbols with `status == "online"`
   - Stores in `v2CompatibleSymbols` list

2. **Discover V1-Compatible Symbols**: Test V2 symbols with V1 endpoints
   - Tests up to 20 V2 symbols with V1 ticker endpoint
   - If V1 accepts the symbol (200 OK), add to `v1CompatibleSymbols`
   - Stops after finding 5 V1-compatible symbols (to avoid rate limiting)

3. **Store Results**:
   ```kotlin
   v1CompatibleSymbols: List<String>  // Symbols that work with V1
   v2CompatibleSymbols: List<String>  // Symbols that work with V2
   ```

#### 2. Test Adaptation

**Location**: `BitgetConnectorIntegrationTest`

**Process**:
1. **Connection Test** (`@Order(2)`):
   - Connects to Bitget
   - Retrieves discovered symbols:
     ```kotlin
     v1TestSymbol = connector.getV1CompatibleSymbol()
     v2TestSymbol = connector.getV2CompatibleSymbol()
     ```

2. **Market Endpoint Tests** (`@Order(3+)`):
   - Use `v1TestSymbol` for V1 market endpoint tests
   - Fallback to `"BTCUSDT"` if no V1-compatible symbol found
   - Example:
     ```kotlin
     val symbol = v1TestSymbol ?: run {
         println("⚠️  No V1-compatible symbol found, falling back to BTCUSDT")
         "BTCUSDT"
     }
     ```

---

## 📊 Symbol Usage Matrix

| Test | API Version | Symbol Source | Fallback |
|------|-------------|---------------|----------|
| `test get candlesticks()` | V1 | `v1TestSymbol` | `BTCUSDT` |
| `test get ticker()` | V1 | `v1TestSymbol` | `BTCUSDT` |
| `test get order book()` | V1 | `v1TestSymbol` | `BTCUSDT` |
| `test WebSocket subscription` | V1 | `v1TestSymbol` | `BTCUSDT` |
| Future V2 tests | V2 | `v2TestSymbol` | `BTCUSDT` |

---

## 🔧 Implementation Details

### Connector Methods

**`BitgetConnector`**:
```kotlin
// Get discovered symbols
fun getV1CompatibleSymbol(): String?
fun getV2CompatibleSymbol(): String?
fun getV1CompatibleSymbols(): List<String>
fun getV2CompatibleSymbols(): List<String>

// Internal discovery (called during onConnect)
private suspend fun discoverV1CompatibleSymbols(candidateSymbols: List<String>)
```

### Test Properties

**`BitgetConnectorIntegrationTest`**:
```kotlin
private var v1TestSymbol: String? = null  // V1-compatible symbol for tests
private var v2TestSymbol: String? = null  // V2-compatible symbol for tests
```

### Test Flow

1. **Setup** (`@BeforeAll`):
   - Create connector and configure
   - Symbols not yet discovered

2. **Connection Test** (`@Order(2)`):
   - Call `connector.connect()`
   - `onConnect()` discovers symbols
   - Store `v1TestSymbol` and `v2TestSymbol`

3. **Market Tests** (`@Order(3+)`):
   - Use `v1TestSymbol` for V1 endpoint tests
   - Tests adapt to available symbols

---

## ✅ Benefits

1. **Adaptive**: Tests automatically use symbols that work with each API version
2. **Resilient**: Falls back gracefully if no compatible symbols found
3. **Future-Proof**: Ready for V2 market endpoints (will use `v2TestSymbol`)
4. **Self-Documenting**: Test output shows which symbols are being used
5. **No Hardcoding**: No need to manually find and update symbol lists

---

## 📝 Example Test Output

```
--- Test: Connection ---
Connecting to Bitget...
✅ Bitget v2 API has 790 available trading pairs
✅ Discovered 790 V2-compatible symbols (online)
   🔍 Discovering V1-compatible symbols...
   ✅ Discovered 3 V1-compatible symbols: ETHUSDT, LUMIAUSDT, GOATUSDT
✅ Connected successfully
✅ Hybrid testing symbols discovered:
   V1-compatible: ETHUSDT, LUMIAUSDT, GOATUSDT
   V2-compatible: 790 symbols
   Using for tests: V1=ETHUSDT, V2=BTCUSDT

--- Test: Get Candlesticks ---
📌 Hybrid Testing: Using V1-compatible symbol
   Using symbol: ETHUSDT (V1-compatible: true)
✅ Received 10 candlesticks
```

---

## 🚀 Future Enhancements

### When V2 Market Endpoints Become Available

1. **Enable V2 in Config**:
   ```kotlin
   bitgetConfig = BitgetConfig(
       // ...
       useV2MarketEndpoints = true  // Enable V2
   )
   ```

2. **Tests Automatically Adapt**:
   - V2 market endpoint tests will use `v2TestSymbol`
   - V1 tests continue using `v1TestSymbol`
   - Both API versions can be tested simultaneously

3. **Hybrid Test Coverage**:
   - Test V1 endpoints with V1-compatible symbols
   - Test V2 endpoints with V2-compatible symbols
   - Verify both API versions work correctly

---

## 📚 Related Documentation

- [BITGET_API_V1_V2_HYBRID_SOLUTION.md](./BITGET_API_V1_V2_HYBRID_SOLUTION.md) - API version strategy
- [DEF_009: Bitget Connector Integration Test Failures](../Defects/DEF_009_Bitget_Connector_Integration_Test_Failures.md) - Defect details
- [Bitget Connector Documentation](../../Development_Handbook/BITGET_CONNECTOR.md) - Connector implementation

---

## 🎯 Summary

The hybrid testing approach provides:
- ✅ **Automatic Symbol Discovery**: Finds compatible symbols during connection
- ✅ **Adaptive Tests**: Use discovered symbols instead of hardcoded values
- ✅ **Graceful Fallback**: Works even if no compatible symbols found
- ✅ **Future-Ready**: Supports both V1 and V2 testing when V2 becomes available
- ✅ **Self-Documenting**: Test output clearly shows which symbols are used

**Status**: ✅ Implemented and ready for testing

