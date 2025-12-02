# Trading Loop Logging Guide

**Version**: 1.0  
**Last Updated**: December 2024  
**Purpose**: Guide for debugging and monitoring the AI Trader trading loop

---

## 📋 **Overview**

This guide explains how to monitor and debug the AI Trader trading process through logs. The trading loop performs several key operations that are logged at different levels.

---

## 📍 **Log File Locations**

### **Development Environment**
- **Console**: Logs are displayed in the console where `core-service` is running
- **File**: `./logs/fmps-autotrader-dev.log`
- **Log Level**: DEBUG (more verbose)

### **Production Environment**
- **Console**: Logs are displayed in the console
- **File**: `./logs/fmps-autotrader.log`
- **Error File**: `./logs/fmps-autotrader-error.log` (errors only)
- **Log Level**: INFO (less verbose, but includes all important events)

---

## 🔍 **What to Look For in Logs**

### **1. Trader Connection to Exchange**

When a trader starts, you should see:

```
INFO  [AITrader] - Starting AI Trader {trader-id} ({trader-name})
INFO  [AITrader] - Connecting to exchange: {BINANCE|BITGET}
INFO  [ExchangeConnector] - Connected to {exchange}
INFO  [AITrader] - AI Trader {trader-id} started successfully
```

**If connection fails:**
```
ERROR [AITrader] - Error starting AI Trader {trader-id}
ERROR [ExchangeConnector] - Failed to connect to {exchange}: {error message}
```

---

### **2. Trading Loop Started**

When the trading loop begins:

```
INFO  [AITrader] - Trading loop started for trader {trader-id}
```

---

### **3. Fetching Market Data (Candlesticks)**

Each iteration of the trading loop fetches market data:

```
DEBUG [AITrader] - Fetching market data for {symbol} on {exchange}
DEBUG [ExchangeConnector] - Fetching candlesticks: symbol={symbol}, interval={interval}, limit={limit}
DEBUG [AITrader] - Fetched {count} candlesticks
```

**If market data fetch fails:**
```
WARN  [AITrader] - No market data available, retrying...
WARN  [ExchangeConnector] - Failed to fetch candlesticks: {error message}
```

---

### **4. Processing Data and Calculating Indicators**

After fetching market data, it's processed:

```
DEBUG [MarketDataProcessor] - Processing {count} candlesticks
DEBUG [MarketDataProcessor] - Calculated SMA(20): {value}
DEBUG [MarketDataProcessor] - Calculated RSI(14): {value}
DEBUG [MarketDataProcessor] - Calculated MACD: signal={signal}, histogram={histogram}
DEBUG [MarketDataProcessor] - Calculated Bollinger Bands: upper={upper}, lower={lower}
DEBUG [AITrader] - Market data processed successfully
```

**If processing fails:**
```
WARN  [AITrader] - Failed to process market data, skipping iteration
WARN  [MarketDataProcessor] - Insufficient data for indicator calculation
```

---

### **5. Generating Trading Signals**

Based on processed data and strategy, signals are generated:

```
DEBUG [SignalGenerator] - Generating signal for {strategy} strategy
DEBUG [SignalGenerator] - Signal confidence: {confidence}%
DEBUG [SignalGenerator] - Signal type: {BUY|SELL|HOLD|CLOSE}
DEBUG [SignalGenerator] - Signal actionable: {true|false}
DEBUG [SignalGenerator] - Signal meets confidence threshold: {true|false}
```

**Example signals:**
```
DEBUG [SignalGenerator] - Trend Following: Uptrend detected, confidence: 75%
DEBUG [SignalGenerator] - Mean Reversion: Oversold condition, confidence: 68%
DEBUG [SignalGenerator] - Breakout: Resistance broken, confidence: 82%
```

---

### **6. Opening Positions**

When a signal is actionable and meets confidence threshold:

```
INFO  [AITrader] - Executing signal: {BUY|SELL}, confidence: {confidence}%
INFO  [AITrader] - Opening {LONG|SHORT} position: symbol={symbol}, quantity={quantity}, leverage={leverage}
DEBUG [ExchangeConnector] - Placing order: {order details}
INFO  [AITrader] - Position opened successfully: positionId={id}
```

**If position opening fails:**
```
ERROR [AITrader] - Failed to open position: {error message}
ERROR [ExchangeConnector] - Order placement failed: {error details}
```

---

### **7. Closing Positions**

When a CLOSE signal is generated or stop-loss/take-profit is hit:

```
INFO  [AITrader] - Closing position: positionId={id}
DEBUG [ExchangeConnector] - Closing order: {order details}
INFO  [AITrader] - Position closed successfully: P&L={profitLoss}
```

---

## 🔧 **Enabling More Detailed Logging**

### **For Development**

The development profile already uses DEBUG level. To see even more details, you can temporarily change the log level in `logback-dev.xml`:

```xml
<logger name="com.fmps.autotrader.core.traders" level="TRACE" />
<logger name="com.fmps.autotrader.core.connectors" level="TRACE" />
```

### **For Production**

To enable DEBUG logging temporarily in production, update the logging level via the Configuration API:

```bash
# Set logging level to DEBUG
curl -X PUT http://localhost:8080/api/v1/config/general \
  -H "Content-Type: application/json" \
  -d '{"loggingLevel": "DEBUG"}'
```

Or use the Desktop UI: **Configuration → General → Logging Level → DEBUG**

---

## 📊 **Monitoring Trading Activity**

### **Real-time Monitoring**

1. **Watch the console** where `core-service` is running
2. **Tail the log file**:
   ```bash
   # Windows PowerShell
   Get-Content -Path ".\logs\fmps-autotrader.log" -Wait -Tail 50
   
   # Linux/Mac
   tail -f logs/fmps-autotrader.log
   ```

### **Key Metrics to Monitor**

- **Trading loop iterations**: Should see regular "Fetching market data" messages
- **Signal generation**: Check signal confidence and type
- **Position opening**: Verify positions are being opened when signals are actionable
- **Errors**: Watch for any ERROR or WARN messages

---

## 🐛 **Common Issues and Debugging**

### **Issue: Trader starts but no market data is fetched**

**Check:**
- Exchange connection status in logs
- API credentials are valid
- Network connectivity to exchange

**Logs to look for:**
```
WARN  [AITrader] - No market data available, retrying...
ERROR [ExchangeConnector] - Failed to fetch candlesticks
```

---

### **Issue: Market data is fetched but no signals are generated**

**Check:**
- Signal confidence threshold (should be >= 70% by default)
- Strategy configuration
- Indicator calculations

**Logs to look for:**
```
DEBUG [SignalGenerator] - Signal confidence: {low value}%
DEBUG [SignalGenerator] - Signal meets confidence threshold: false
```

---

### **Issue: Signals are generated but no positions are opened**

**Check:**
- Signal is actionable (`isActionable() == true`)
- Signal meets confidence threshold
- Exchange order placement permissions
- Risk limits (max positions, daily loss limits)

**Logs to look for:**
```
DEBUG [SignalGenerator] - Signal actionable: false
INFO  [AITrader] - Signal generated but not actionable
```

---

## 📝 **Log Format**

Logs follow this format:

```
{timestamp} {LEVEL} [{logger}] - {message}
```

**Example:**
```
2024-12-15 14:30:25.123 INFO  [AITrader] - Trading loop started for trader trader-1
2024-12-15 14:30:25.456 DEBUG [ExchangeConnector] - Fetching candlesticks: symbol=BTCUSDT, interval=1h, limit=100
2024-12-15 14:30:25.789 DEBUG [MarketDataProcessor] - Calculated RSI(14): 45.2
2024-12-15 14:30:25.890 DEBUG [SignalGenerator] - Signal confidence: 75%
2024-12-15 14:30:25.901 INFO  [AITrader] - Opening LONG position: symbol=BTCUSDT, quantity=0.02
```

---

## 🔗 **Related Documentation**

- [AI Trader Core Guide](../Development_Handbook/AI_TRADER_CORE_GUIDE.md)
- [Configuration Guide](../Development_Handbook/CONFIG_GUIDE.md)
- [API Documentation](../../core-service/API_DOCUMENTATION.md)

---

## 📞 **Support**

If you encounter issues not covered in this guide, check:
1. Error logs in `./logs/fmps-autotrader-error.log`
2. Core service console output
3. Exchange API status and rate limits

