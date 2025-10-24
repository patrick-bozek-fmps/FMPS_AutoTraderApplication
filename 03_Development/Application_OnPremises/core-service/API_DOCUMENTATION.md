# FMPS AutoTrader API Documentation

**Version**: 1.0.0-SNAPSHOT  
**Base URL**: `http://localhost:8080`  
**Content-Type**: `application/json`

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Response Format](#response-format)
4. [Health & Status](#health--status)
5. [AI Trader Management](#ai-trader-management)
6. [Trade Management](#trade-management)
7. [Pattern Management](#pattern-management)
8. [Configuration Management](#configuration-management)
9. [WebSocket Real-Time Updates](#websocket-real-time-updates)
10. [Error Codes](#error-codes)

---

## Overview

The FMPS AutoTrader REST API provides programmatic access to manage AI traders, trades, patterns, and system configuration. The API follows REST principles and returns JSON responses.

### Base URL
```
http://localhost:8080
```

### API Version
All API endpoints are versioned under `/api/v1/`

---

## Authentication

⚠️ **Note**: Authentication is not yet implemented. All endpoints are currently accessible without authentication.

*Planned for Phase 4: JWT token-based authentication*

---

## Response Format

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "timestamp": 1698153600000
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": ["Additional error details"]
  },
  "timestamp": 1698153600000
}
```

---

## Health & Status

### GET /api/health
Check if the API server is running.

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2025-10-24T12:00:00.000Z",
  "uptime": 3600000
}
```

### GET /api/status
Get detailed system status including database and component health.

**Response:**
```json
{
  "status": "healthy",
  "components": {
    "database": "connected",
    "aiTraders": 3,
    "activeTrades": 5
  },
  "system": {
    "uptime": 3600000,
    "memory": {
      "free": 512000000,
      "total": 1024000000,
      "max": 2048000000
    },
    "database": {
      "activeConnections": 5,
      "totalConnections": 10
    }
  },
  "timestamp": "2025-10-24T12:00:00.000Z"
}
```

### GET /api/version
Get API version information.

**Response:**
```json
{
  "version": "1.0.0-SNAPSHOT",
  "buildTime": "2025-10-24T10:00:00.000Z",
  "apiVersion": "v1"
}
```

---

## AI Trader Management

### GET /api/v1/traders
Get all AI traders.

**Query Parameters:**
- `limit` (optional): Maximum number of results (default: 100)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "BTC Trader 01",
      "exchange": "BINANCE",
      "tradingPair": "BTC/USDT",
      "status": "ACTIVE",
      "leverage": 10,
      "stopLossPercentage": "0.02",
      "takeProfitPercentage": "0.05",
      "initialBalance": "1000.00",
      "currentBalance": "1050.50",
      "totalProfit": "50.50",
      "totalLoss": "0.00",
      "winCount": 3,
      "lossCount": 0,
      "createdAt": "2025-10-24T10:00:00.000Z",
      "updatedAt": "2025-10-24T12:00:00.000Z",
      "lastActiveAt": "2025-10-24T12:00:00.000Z"
    }
  ],
  "timestamp": 1698153600000
}
```

### GET /api/v1/traders/:id
Get a specific AI trader by ID.

**Response:** Same structure as single trader in list above.

### POST /api/v1/traders
Create a new AI trader.

**Request Body:**
```json
{
  "name": "BTC Trader 01",
  "exchange": "BINANCE",
  "tradingPair": "BTC/USDT",
  "leverage": 10,
  "stopLossPercentage": "0.02",
  "takeProfitPercentage": "0.05",
  "initialBalance": "1000.00"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "BTC Trader 01",
    ...
  },
  "timestamp": 1698153600000
}
```

### PATCH /api/v1/traders/:id/status
Update AI trader status.

**Request Body:**
```json
{
  "status": "PAUSED"
}
```

**Valid statuses**: `IDLE`, `ACTIVE`, `PAUSED`, `ERROR`

### PATCH /api/v1/traders/:id/balance
Update AI trader balance.

**Request Body:**
```json
{
  "balance": "1200.50"
}
```

### GET /api/v1/traders/active
Get all active AI traders.

**Response:** Array of traders with status = "ACTIVE"

### GET /api/v1/traders/can-create
Check if a new AI trader can be created (max 3 active traders).

**Response:**
```json
{
  "success": true,
  "data": {
    "canCreate": true,
    "activeCount": 2,
    "maxAllowed": 3
  },
  "timestamp": 1698153600000
}
```

---

## Trade Management

### GET /api/v1/trades
Get all trades (with optional limit).

**Query Parameters:**
- `limit` (optional): Maximum number of results (default: 100)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "aiTraderId": 1,
      "exchange": "BINANCE",
      "tradingPair": "BTC/USDT",
      "side": "LONG",
      "entryPrice": "50000.00",
      "exitPrice": "51000.00",
      "quantity": "0.02",
      "leverage": 10,
      "stopLoss": "49000.00",
      "takeProfit": "52000.00",
      "status": "CLOSED",
      "profitLoss": "20.00",
      "profitLossPercentage": "2.00",
      "entryReason": "Pattern detected",
      "exitReason": "Take profit hit",
      "patternId": 1,
      "entryTime": "2025-10-24T10:00:00.000Z",
      "exitTime": "2025-10-24T11:00:00.000Z",
      "duration": 3600000
    }
  ],
  "timestamp": 1698153600000
}
```

### GET /api/v1/trades/:id
Get a specific trade by ID.

### POST /api/v1/trades
Create a new trade (open a position).

**Request Body:**
```json
{
  "aiTraderId": 1,
  "exchange": "BINANCE",
  "tradingPair": "BTC/USDT",
  "side": "LONG",
  "entryPrice": "50000.00",
  "quantity": "0.02",
  "leverage": 10,
  "stopLoss": "49000.00",
  "takeProfit": "52000.00",
  "entryReason": "Pattern detected",
  "patternId": 1
}
```

### POST /api/v1/trades/:id/close
Close an open trade.

**Request Body:**
```json
{
  "exitPrice": "51000.00",
  "exitReason": "Take profit hit"
}
```

### PATCH /api/v1/trades/:id/stop-loss
Update stop-loss price for an open trade.

**Request Body:**
```json
{
  "stopLoss": "49500.00"
}
```

### GET /api/v1/trades/open
Get all currently open trades.

### GET /api/v1/trades/trader/:traderId
Get all trades for a specific AI trader.

### GET /api/v1/trades/statistics
Get overall trade statistics.

**Query Parameters:**
- `traderId` (optional): Filter by specific trader

**Response:**
```json
{
  "success": true,
  "data": {
    "aiTraderId": 1,
    "totalTrades": 20,
    "openTrades": 5,
    "closedTrades": 15,
    "winningTrades": 12,
    "losingTrades": 3,
    "winRate": 80.0,
    "totalProfit": "500.00",
    "totalLoss": "100.00",
    "netProfitLoss": "400.00",
    "averageProfit": "41.67",
    "profitFactor": 5.0,
    "averageTradeDuration": 3600000,
    "longestTrade": 7200000,
    "shortestTrade": 1800000,
    "largestWin": "100.00",
    "largestLoss": "50.00"
  },
  "timestamp": 1698153600000
}
```

---

## Pattern Management

### GET /api/v1/patterns
Get all trading patterns.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "RSI Oversold Reversal",
      "description": "RSI below 30 with bullish candlestick",
      "exchange": "BINANCE",
      "tradingPair": "BTC/USDT",
      "indicators": "{\"rsi\":{\"period\":14,\"threshold\":30}}",
      "candlestickPattern": "HAMMER",
      "successRate": "75.50",
      "totalTrades": 20,
      "winningTrades": 15,
      "losingTrades": 5,
      "averageProfit": "45.00",
      "averageLoss": "20.00",
      "isActive": true,
      "createdAt": "2025-10-24T10:00:00.000Z",
      "updatedAt": "2025-10-24T12:00:00.000Z",
      "lastUsedAt": "2025-10-24T11:30:00.000Z"
    }
  ],
  "timestamp": 1698153600000
}
```

### GET /api/v1/patterns/:id
Get a specific pattern by ID.

### POST /api/v1/patterns
Create a new trading pattern.

**Request Body:**
```json
{
  "name": "RSI Oversold Reversal",
  "description": "RSI below 30 with bullish candlestick",
  "exchange": "BINANCE",
  "tradingPair": "BTC/USDT",
  "indicators": "{\"rsi\":{\"period\":14,\"threshold\":30}}",
  "candlestickPattern": "HAMMER"
}
```

### PATCH /api/v1/patterns/:id/statistics
Update pattern statistics after a trade.

**Request Body:**
```json
{
  "success": true,
  "profitLoss": "45.50"
}
```

### GET /api/v1/patterns/active
Get all active patterns (isActive = true).

### GET /api/v1/patterns/match
Find patterns matching current market conditions.

**Query Parameters:**
- `exchange`: Exchange name (required)
- `tradingPair`: Trading pair symbol (required)
- `rsi`: Current RSI value (optional)
- `macd`: Current MACD value (optional)

### GET /api/v1/patterns/top
Get top performing patterns.

**Query Parameters:**
- `limit`: Number of results (default: 10)

### PATCH /api/v1/patterns/:id/activate
Activate a pattern.

### PATCH /api/v1/patterns/:id/deactivate
Deactivate a pattern.

### DELETE /api/v1/patterns/:id
Delete a pattern (soft delete).

---

## Configuration Management

⚠️ **Note**: Configuration endpoints are currently placeholders. Full implementation planned for Phase 4.

### GET /api/v1/config
Get all configuration settings.

### GET /api/v1/config/:key
Get configuration by key.

### PUT /api/v1/config/:key
Update configuration setting.

### GET /api/v1/config/category/:category
Get configurations by category.

---

## WebSocket Real-Time Updates

### Trader Status Updates
**URL**: `ws://localhost:8080/ws/trader-status`

Subscribe to real-time AI trader status updates.

**Message Format:**
```json
{
  "type": "trader-status-update",
  "channel": "trader-status",
  "data": "{\"traderId\":1,\"status\":\"ACTIVE\",\"details\":{...}}",
  "timestamp": 1698153600000
}
```

### Trade Updates
**URL**: `ws://localhost:8080/ws/trades`

Subscribe to real-time trade updates (open, close, update).

**Message Format:**
```json
{
  "type": "trade-update",
  "channel": "trades",
  "data": "{\"tradeId\":1,\"action\":\"OPENED\",\"details\":{...}}",
  "timestamp": 1698153600000
}
```

### Market Data Updates
**URL**: `ws://localhost:8080/ws/market-data`

Subscribe to real-time market data updates (placeholder).

**Message Format:**
```json
{
  "type": "market-data-update",
  "channel": "market-data",
  "data": "{\"symbol\":\"BTC/USDT\",\"price\":50000.00,\"details\":{...}}",
  "timestamp": 1698153600000
}
```

### Connection Example (JavaScript)
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/trades');

ws.onopen = () => {
  console.log('Connected to trade updates');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Trade update:', message);
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

ws.onclose = () => {
  console.log('Disconnected from trade updates');
};

// Ping to keep connection alive
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send('ping');
  }
}, 30000);
```

### WebSocket Statistics
**URL**: `GET /api/v1/websocket/stats`

Get current WebSocket connection statistics.

**Response:**
```json
{
  "success": true,
  "data": {
    "totalConnections": 5,
    "traderStatusSubscribers": 2,
    "tradeSubscribers": 2,
    "marketDataSubscribers": 1
  },
  "timestamp": 1698153600000
}
```

---

## Error Codes

| Code | Description |
|------|-------------|
| `TRADER_NOT_FOUND` | AI Trader with specified ID not found |
| `TRADER_LIMIT_REACHED` | Maximum number of active traders (3) reached |
| `INVALID_STATUS` | Invalid trader status provided |
| `INVALID_BALANCE` | Invalid balance amount |
| `TRADE_NOT_FOUND` | Trade with specified ID not found |
| `TRADE_ALREADY_CLOSED` | Cannot modify a closed trade |
| `INVALID_PRICE` | Invalid price value |
| `PATTERN_NOT_FOUND` | Pattern with specified ID not found |
| `INVALID_INDICATORS` | Invalid indicator JSON format |
| `DATABASE_ERROR` | Internal database error occurred |
| `VALIDATION_ERROR` | Request validation failed |
| `SERVER_ERROR` | Internal server error |

---

## Usage Examples

### Example 1: Create and Manage an AI Trader

```bash
# Create a new AI trader
curl -X POST http://localhost:8080/api/v1/traders \
  -H "Content-Type: application/json" \
  -d '{
    "name": "BTC Scalper",
    "exchange": "BINANCE",
    "tradingPair": "BTC/USDT",
    "leverage": 10,
    "stopLossPercentage": "0.02",
    "takeProfitPercentage": "0.05",
    "initialBalance": "1000.00"
  }'

# Get trader details
curl http://localhost:8080/api/v1/traders/1

# Activate the trader
curl -X PATCH http://localhost:8080/api/v1/traders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}'
```

### Example 2: Open and Close a Trade

```bash
# Open a new trade
curl -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d '{
    "aiTraderId": 1,
    "exchange": "BINANCE",
    "tradingPair": "BTC/USDT",
    "side": "LONG",
    "entryPrice": "50000.00",
    "quantity": "0.02",
    "leverage": 10,
    "stopLoss": "49000.00",
    "takeProfit": "52000.00",
    "entryReason": "RSI oversold",
    "patternId": 1
  }'

# Close the trade
curl -X POST http://localhost:8080/api/v1/trades/1/close \
  -H "Content-Type: application/json" \
  -d '{
    "exitPrice": "51000.00",
    "exitReason": "Take profit hit"
  }'
```

### Example 3: Query Trade Statistics

```bash
# Get overall statistics
curl http://localhost:8080/api/v1/trades/statistics

# Get statistics for specific trader
curl http://localhost:8080/api/v1/trades/statistics?traderId=1
```

---

## Rate Limiting

⚠️ **Note**: Rate limiting is not yet implemented.

*Planned for Phase 4: 100 requests per minute per IP*

---

## Support

For issues or questions:
- GitHub Issues: [Project Repository]
- Documentation: This file
- Email: [Your Contact]

---

**Last Updated**: October 24, 2025  
**Version**: 1.0.0-SNAPSHOT

