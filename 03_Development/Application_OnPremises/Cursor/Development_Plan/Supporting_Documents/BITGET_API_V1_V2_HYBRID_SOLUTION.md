# Bitget API V1/V2 Hybrid Solution

**Status**: ✅ Implemented  
**Date**: November 19, 2025  
**Related Defect**: [DEF_009](../Defects/DEF_009_Bitget_Connector_Integration_Test_Failures.md)

---

## 📋 Overview

This document describes the hybrid solution implemented to handle Bitget's API version transition, where V1 is deprecated but V2 spot market endpoints are not yet available.

---

## 🔍 Problem Statement

**Bitget API Status (as of Nov 2025)**:
- ✅ **V2 Symbols Endpoint**: Available and working (`/api/v2/spot/public/symbols`)
  - **Note**: This is a **public information endpoint**, not a "market operation"
  - Used to query available trading pairs (informational)
- ❌ **V2 Spot Market Endpoints**: Not available yet (return 404)
  - **Note**: These are actual "spot market operations" (candles, ticker, order book)
  - Per Bitget: "spot market operations must still use V1 endpoints"
- ⚠️ **V1 API**: Deprecated (discontinued Nov 28, 2025) but still required for spot market operations
- ✅ **V2 Futures/Contracts**: Available and working

**Challenge**: 
- Use V2 for public information endpoints (symbols) - not a "market operation"
- Must use deprecated V1 for actual spot market operations (per Bitget's statement)
- Need easy migration path when V2 spot market endpoints are released (if/when they become available)

---

## 💡 Solution: Hybrid API Version Strategy

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│              BitgetConnector                            │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │      buildEndpointUrl() Helper Method           │   │
│  │  - EndpointType.SYMBOLS → Always V2             │   │
│  │  - EndpointType.MARKET → V1 or V2 (configurable)│   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────┐  ┌──────────────────────────┐   │
│  │  BitgetConfig    │  │  useV2MarketEndpoints     │   │
│  │  - useV2Market   │  │  = false (default)        │   │
│  │    Endpoints     │  │  = true (when V2 ready)   │   │
│  └──────────────────┘  └──────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Implementation Details

#### 1. Configuration Flag

**`BitgetConfig.useV2MarketEndpoints`**:
- **Default**: `false` (V2 spot market endpoints not available yet)
- **When to enable**: Set to `true` **only if/when** Bitget releases V2 spot market endpoints
- **Note**: Per Bitget's statement, "spot market operations must still use V1 endpoints" - this flag is for future use if V2 becomes available
- **Migration**: Single config change enables V2 for all market endpoints (if/when available)

```kotlin
data class BitgetConfig(
    // ... other fields ...
    val useV2MarketEndpoints: Boolean = false  // Default: false
)
```

#### 2. Endpoint Builder

**`buildEndpointUrl()`** method:
- **Symbols endpoints**: Always uses V2 (`/api/v2/spot/public/symbols`)
  - **Rationale**: This is a **public information endpoint** (not a "market operation")
  - Per Bitget: Only "spot market operations" must use V1, not public info endpoints
- **Market endpoints**: Uses V1 by default, V2 when enabled
  - V1: `/api/spot/v1/market/{endpoint}` (required per Bitget's statement)
  - V2: `/api/v2/spot/market/{endpoint}` (when enabled, if/when available)

```kotlin
private fun buildEndpointUrl(
    endpointType: EndpointType, 
    path: String, 
    queryParams: Map<String, String> = emptyMap()
): String {
    when (endpointType) {
        EndpointType.SYMBOLS -> "/api/v2/spot/public/$path"  // Always V2
        EndpointType.MARKET -> {
            if (bitgetConfig.useV2MarketEndpoints) {
                "/api/v2/spot/market/$path"  // V2 (when enabled)
            } else {
                "/api/spot/v1/market/$path"  // V1 (default)
            }
        }
    }
}
```

#### 3. Current Endpoint Usage

| Endpoint | Current Version | Path | Type |
|----------|----------------|------|------|
| Symbols | V2 (always) | `/api/v2/spot/public/symbols` | Public Info (not a market operation) |
| Candles | V1 (required) | `/api/spot/v1/market/candles` | Spot Market Operation |
| Ticker | V1 (required) | `/api/spot/v1/market/ticker` | Spot Market Operation |
| Order Book | V1 (required) | `/api/spot/v1/market/depth` | Spot Market Operation |

**Note**: Per Bitget's statement: "spot market operations must still use V1 endpoints"

---

## 🚀 Migration Path

### ⚠️ Important Note

**Per Bitget's Official Statement**: "spot market operations must still use V1 endpoints"

This means:
- ✅ **Current Strategy is Correct**: Using V1 for spot market operations (candles, ticker, order book)
- ✅ **Symbols Endpoint**: Using V2 is correct (it's public info, not a market operation)
- ⚠️ **Future Migration**: The `useV2MarketEndpoints` flag is for **future use only** if Bitget releases V2 spot market endpoints (which may not happen)

### If V2 Spot Market Endpoints Become Available (Future)

**Step 1**: Verify V2 endpoints are working
```kotlin
// Test V2 endpoint manually
val testUrl = "https://api.bitget.com/api/v2/spot/market/ticker?symbol=BTCUSDT"
// Should return 200 OK (not 404)
```

**Step 2**: Enable V2 in configuration
```kotlin
// In BitgetConfig.kt or configuration source
bitgetConfig = BitgetConfig(
    baseExchangeConfig = config,
    passphrase = config.passphrase!!,
    useV2MarketEndpoints = true  // ✅ Enable V2
)
```

**Step 3**: All market endpoints automatically switch to V2
- No code changes needed
- All endpoints use `buildEndpointUrl()` which respects the config flag

**Step 4**: Test and verify
- Run integration tests
- Verify all market endpoints work with V2
- Monitor for any V2-specific parameter differences

---

## ✅ Benefits

1. **Aligned with Bitget's Statement**: Uses V1 for spot market operations (as required), V2 for public info
2. **Future-Proof**: Easy migration if V2 becomes available (single config change)
3. **Hybrid Approach**: Uses V2 where appropriate (public info), V1 where required (market operations)
4. **No Code Changes**: Migration requires only configuration update (if/when needed)
5. **Centralized Logic**: All endpoint building in one place (`buildEndpointUrl()`)
6. **Clear Documentation**: Code comments explain V1/V2 status and alignment with Bitget's requirements

---

## 📝 Code Examples

### Current Usage (V1 for market endpoints)

```kotlin
// Symbols endpoint - always V2
val symbolsUrl = buildEndpointUrl(EndpointType.SYMBOLS, "symbols")
// Result: /api/v2/spot/public/symbols

// Market endpoints - V1 (default)
val tickerUrl = buildEndpointUrl(EndpointType.MARKET, "ticker", mapOf("symbol" to "BTCUSDT"))
// Result: /api/spot/v1/market/ticker?symbol=BTCUSDT
```

### After V2 Migration (when enabled)

```kotlin
// Same code, different result
val tickerUrl = buildEndpointUrl(EndpointType.MARKET, "ticker", mapOf("symbol" to "BTCUSDT"))
// Result: /api/v2/spot/market/ticker?symbol=BTCUSDT  (V2!)
```

---

## 🔄 Monitoring & Updates

### How to Check V2 Availability

1. **Monitor Bitget API Documentation**: Check for V2 spot market endpoint announcements
2. **Test Endpoints**: Periodically test V2 endpoints:
   ```bash
   curl "https://api.bitget.com/api/v2/spot/market/ticker?symbol=BTCUSDT"
   ```
3. **Check Integration Tests**: When V2 is available, tests should pass with `useV2MarketEndpoints = true`

### Update Checklist

When V2 spot market endpoints become available:

- [ ] Verify V2 endpoints return 200 OK (not 404)
- [ ] Test with real API keys
- [ ] Check for parameter differences between V1 and V2
- [ ] Update `useV2MarketEndpoints = true` in configuration
- [ ] Run full integration test suite
- [ ] Update this document with V2 availability date
- [ ] Update DEF_009 with resolution

---

## 📚 Related Documentation

- [DEF_009: Bitget Connector Integration Test Failures](../Defects/DEF_009_Bitget_Connector_Integration_Test_Failures.md)
- [Bitget Connector Documentation](../../Development_Handbook/BITGET_CONNECTOR.md)
- [Bitget API Official Documentation](https://www.bitget.com/api-doc)

---

## 🎯 Summary

The hybrid solution provides:
- ✅ **Aligned with Bitget**: Uses V1 for spot market operations (as required), V2 for public info endpoints
- ✅ **Current**: Uses V2 for symbols (public info), V1 for market endpoints (required)
- ✅ **Future**: Easy migration to V2 if/when available (single config flag)
- ✅ **Maintainable**: Centralized endpoint building logic
- ✅ **Documented**: Clear alignment with Bitget's requirements and migration path

**Status**: 
- ✅ **Correctly aligned** with Bitget's statement: "spot market operations must still use V1 endpoints"
- ✅ **Symbols endpoint** uses V2 (public info, not a market operation)
- ✅ **Market endpoints** use V1 (as required by Bitget)
- ⚠️ **Future migration** ready if Bitget releases V2 spot market endpoints

