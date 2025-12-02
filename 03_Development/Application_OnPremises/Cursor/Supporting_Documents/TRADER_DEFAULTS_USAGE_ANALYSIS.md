# Trader Defaults Usage Analysis

## Overview
This document analyzes the current usage of Trader Defaults (Default Budget, Leverage, Stop Loss, Default Strategy) and identifies gaps where these values should be used but are currently not.

## Current State

### 1. Desktop UI Configuration
- **Location**: `ConfigurationView.kt` → Trader Defaults tab
- **Fields**:
  - Default Budget (USDT): `budgetUsd: Double` (default: 1000.0)
  - Leverage: `leverage: Int` (default: 3)
  - Stop Loss (%): `stopLossPercent: Double` (default: 5.0)
  - Default Strategy: `strategy: String` (default: "Momentum") ⚠️ **ISSUE: Should be TradingStrategy enum**

### 2. Data Model
- **Location**: `ConfigurationContract.kt`
- **TraderDefaultsForm**: Contains the four fields above
- **TraderDefaults**: Service layer DTO (same fields)

### 3. API Endpoint
- **Expected**: `PUT /api/v1/config/trader-defaults`
- **Status**: ⚠️ **MISSING** - Currently returns `501 Not Implemented` or falls back to local file storage
- **Location**: `RealConfigService.kt` line 226

### 4. Core Service Usage

#### A. AI Trader Creation (`AITraderRoutes.kt`)
- **Endpoint**: `POST /api/v1/traders`
- **Request DTO**: `CreateAITraderRequest`
  - Contains: `leverage`, `stopLossPercentage`, `initialBalance`
  - ⚠️ **MISSING**: `strategy` field
- **Repository**: `AITraderRepository.create()`
  - Uses values from request directly
  - ⚠️ **NOT USING**: Trader Defaults as fallback when values are not provided

#### B. Database Schema (`AITradersTable.kt`)
- **Columns**:
  - `leverage: Int` (default: 10)
  - `initial_balance: BigDecimal`
  - `stop_loss_percent: BigDecimal` (default: 0.02)
  - ⚠️ **MISSING**: `strategy` column

#### C. Config Files (`application.conf`, `reference.conf`)
- **Location**: `aiTrader.defaultRisk`
  - `maxLeverage = 10`
  - `defaultStopLossPercentage = 0.02`
  - `defaultTakeProfitPercentage = 0.05`
- ⚠️ **MISSING**: Default strategy configuration
- ⚠️ **MISSING**: Default budget/initial balance

## Issues Identified

### Issue 1: Strategy Field Type Mismatch
- **Current**: `strategy: String` with default "Momentum"
- **Expected**: `strategy: TradingStrategy` enum (TREND_FOLLOWING, MEAN_REVERSION, BREAKOUT)
- **Impact**: Invalid default value, no type safety
- **Status**: ✅ **FIXED** - Updated to use TradingStrategy enum with "TREND_FOLLOWING" as default

### Issue 1a: Strategy Inconsistency Between Views
- **Problem**: AI Traders view showed 4 strategies ("Momentum", "Mean Reversion", "Arbitrage", "Scalping") but only 3 are implemented
- **Root Cause**: `TraderManagementView` had hardcoded strategy list that didn't match the actual `TradingStrategy` enum
- **Impact**: Users could select strategies that don't exist in the backend
- **Status**: ✅ **FIXED** - Both views now use the same TradingStrategy enum values

### Issue 2: Missing API Endpoint
- **Current**: `/api/v1/config/trader-defaults` returns 501 or falls back to file
- **Expected**: Should be implemented similar to `/api/v1/config/general`
- **Impact**: Trader defaults not persisted in core-service

### Issue 3: Trader Defaults Not Used When Creating Traders
- **Current**: `CreateAITraderRequest` requires all values explicitly
- **Expected**: Should use Trader Defaults as fallback when values are not provided
- **Impact**: Users must always specify all values, defaults are ignored

### Issue 4: Strategy Not Stored in Database
- **Current**: Database schema doesn't have a `strategy` column
- **Expected**: Should store strategy per trader
- **Impact**: Strategy cannot be persisted or retrieved per trader

### Issue 5: No Runtime Configuration Manager for Trader Defaults
- **Current**: Only `RuntimeConfigManager` for GeneralSettings
- **Expected**: Should store Trader Defaults in `RuntimeConfigManager`
- **Impact**: Cannot update trader defaults at runtime

## Required Changes

### 1. Desktop UI Changes
- [ ] Change `strategyField` from `TextField` to `ComboBox<TradingStrategy>`
- [ ] Update `TraderDefaultsForm.strategy` to use `TradingStrategy` enum
- [ ] Update validation to use enum instead of string
- [ ] Map enum values to user-friendly display names

### 2. Core Service API Changes
- [ ] Create `PUT /api/v1/config/trader-defaults` endpoint in `ConfigurationRoutes.kt`
- [ ] Create `TraderDefaultsDTO` for request/response
- [ ] Add `TraderDefaults` to `RuntimeConfigManager`
- [ ] Initialize `RuntimeConfigManager` with trader defaults from config

### 3. Database Schema Changes (Future)
- [ ] Add `strategy` column to `ai_traders` table (migration)
- [ ] Update `AITradersTable` schema
- [ ] Update `AITraderRepository` to handle strategy

### 4. AI Trader Creation Changes (Future)
- [ ] Update `CreateAITraderRequest` to make fields optional
- [ ] Use Trader Defaults as fallback when values are not provided
- [ ] Add `strategy` field to request

## Implementation Priority

### Phase 1: Immediate (This Task)
1. ✅ Change strategy field to ComboBox with TradingStrategy enum
2. ✅ Create `/api/v1/config/trader-defaults` endpoint
3. ✅ Add TraderDefaults to RuntimeConfigManager
4. ✅ Ensure values are persisted and loaded correctly

### Phase 2: Future Enhancements
1. Add strategy column to database
2. Use Trader Defaults as fallback in trader creation
3. Make trader creation fields optional

## Testing Checklist

- [ ] Strategy dropdown shows all TradingStrategy enum values
- [ ] Selected strategy is saved and loaded correctly
- [ ] Trader defaults are sent to core-service API
- [ ] Core-service stores trader defaults in RuntimeConfigManager
- [ ] Trader defaults persist across restarts
- [ ] Validation works for all four fields

