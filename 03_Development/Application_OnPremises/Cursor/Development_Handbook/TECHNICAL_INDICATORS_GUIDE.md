# Technical Indicators Guide

**Version**: 1.0  
**Last Updated**: October 30, 2025  
**Module**: `core-service/indicators`

---

## 📋 **Overview**

The Technical Indicators module provides a comprehensive library of technical analysis indicators for the FMPS AutoTrader system. These indicators analyze historical price data to identify trends, momentum, volatility, and potential trading signals.

### **Available Indicators**

1. **SMA** (Simple Moving Average) - Trend identification
2. **EMA** (Exponential Moving Average) - Responsive trend tracking
3. **RSI** (Relative Strength Index) - Momentum oscillator
4. **MACD** (Moving Average Convergence Divergence) - Trend and momentum
5. **Bollinger Bands** - Volatility and price range

---

## 🎯 **Quick Start**

### **Basic Usage with Extension Functions**

```kotlin
import com.fmps.autotrader.core.indicators.*
import com.fmps.autotrader.shared.model.Candlestick

// Assuming you have a list of candlesticks
val candles: List<Candlestick> = fetchCandles("BTC/USDT", TimeFrame.ONE_HOUR, 100)

// Calculate Simple Moving Average
val sma20 = candles.sma(period = 20)
println("SMA(20): $sma20")

// Calculate RSI
val rsi = candles.rsi(period = 14)
if (RSIIndicator.isOverbought(rsi!!)) {
    println("RSI indicates overbought condition")
}

// Calculate MACD
val macd = candles.macd()
if (macd?.isBullish() == true) {
    println("MACD shows bullish momentum")
}

// Calculate Bollinger Bands
val bb = candles.bollingerBands(period = 20, stdDevMultiplier = 2.0)
if (bb?.isSqueeze() == true) {
    println("Bollinger Bands are squeezing - volatility breakout likely")
}
```

### **Using Indicator Classes Directly**

```kotlin
val smaIndicator = SMAIndicator(period = 50)
val result = smaIndicator.calculate(candles)

if (result != null) {
    println("SMA(50) = $result")
}
```

---

## 📊 **Indicator Details**

### **1. SMA (Simple Moving Average)**

**Purpose**: Smooths price data to identify trend direction.

**Formula**:
```
SMA = (Sum of closing prices over N periods) / N
```

**Usage**:
```kotlin
// Single value
val sma = candles.sma(period = 20)

// Full series
val smaAll = candles.smaAll(period = 20)

// Direct instantiation
val indicator = SMAIndicator(period = 50)
val result = indicator.calculate(candles)
```

**Parameters**:
- `period`: Number of periods (default: 20)

**Interpretation**:
- Price above SMA → Uptrend
- Price below SMA → Downtrend
- SMA acts as dynamic support/resistance

**Best For**:
- Trend identification
- Support/resistance levels
- Golden Cross / Death Cross strategies

---

### **2. EMA (Exponential Moving Average)**

**Purpose**: More responsive moving average that gives more weight to recent prices.

**Formula**:
```
Multiplier (α) = 2 / (period + 1)
EMA(today) = (Close(today) - EMA(yesterday)) × α + EMA(yesterday)
First EMA = SMA of first 'period' data points
```

**Usage**:
```kotlin
// Single value
val ema = candles.ema(period = 12)

// Full series
val emaAll = candles.emaAll(period = 12)

// Direct instantiation
val indicator = EMAIndicator(period = 26)
val result = indicator.calculate(candles)
```

**Parameters**:
- `period`: Number of periods (default: 12)

**Interpretation**:
- Price above EMA → Uptrend
- Price below EMA → Downtrend
- Reacts faster to price changes than SMA

**Best For**:
- Short-term trend identification
- Component of MACD
- Dynamic support/resistance

**Common Periods**:
- EMA(12), EMA(26) - Used in MACD
- EMA(50), EMA(200) - Long-term trends

---

### **3. RSI (Relative Strength Index)**

**Purpose**: Momentum oscillator measuring speed and magnitude of price changes.

**Formula**:
```
Average Gain = EMA of gains over period
Average Loss = EMA of losses over period
RS = Average Gain / Average Loss
RSI = 100 - (100 / (1 + RS))
```

**Usage**:
```kotlin
// Single value
val rsi = candles.rsi(period = 14)

// Full series
val rsiAll = candles.rsiAll(period = 14)

// Check conditions
if (candles.isRSIOverbought()) {
    println("RSI > 70: Overbought")
}
if (candles.isRSIOversold()) {
    println("RSI < 30: Oversold")
}

// Custom thresholds
if (RSIIndicator.isOverbought(rsi!!, threshold = 75.0)) {
    println("Strong overbought signal")
}
```

**Parameters**:
- `period`: Number of periods (default: 14)

**Interpretation**:
- RSI > 70 → Overbought (potential sell signal)
- RSI < 30 → Oversold (potential buy signal)
- RSI = 50 → Neutral momentum
- Divergence → Potential reversal

**Best For**:
- Identifying overbought/oversold conditions
- Momentum confirmation
- Divergence trading

**Tips**:
- Works best in ranging markets
- In strong trends, RSI can stay overbought/oversold for extended periods
- Combine with trend indicators for confirmation

---

### **4. MACD (Moving Average Convergence Divergence)**

**Purpose**: Trend-following momentum indicator showing relationship between two EMAs.

**Formula**:
```
MACD Line = EMA(12) - EMA(26)
Signal Line = EMA(9) of MACD Line
Histogram = MACD Line - Signal Line
```

**Usage**:
```kotlin
// Single value
val macd = candles.macd(
    fastPeriod = 12,
    slowPeriod = 26,
    signalPeriod = 9
)

// Check components
if (macd != null) {
    println("MACD: ${macd.macd}")
    println("Signal: ${macd.signal}")
    println("Histogram: ${macd.histogram}")
    
    // Interpretation
    if (macd.isBullish()) {
        println("MACD above signal - bullish")
    }
    
    if (macd.hasPositiveMomentum()) {
        println("Histogram positive - momentum increasing")
    }
}

// Detect crossovers
val macdAll = candles.macdAll()
for (i in 1 until macdAll.size) {
    val current = macdAll[i]
    val previous = macdAll[i - 1]
    
    if (current != null && previous != null) {
        if (current.isBullishCrossover(previous)) {
            println("Bullish crossover at index $i")
        }
    }
}

// Convenience functions
if (candles.isMACDBullish()) {
    println("MACD momentum is bullish")
}
```

**Parameters**:
- `fastPeriod`: Fast EMA period (default: 12)
- `slowPeriod`: Slow EMA period (default: 26)
- `signalPeriod`: Signal line period (default: 9)

**Interpretation**:
- MACD > Signal → Bullish momentum
- MACD < Signal → Bearish momentum
- MACD crosses above Signal → Buy signal
- MACD crosses below Signal → Sell signal
- Histogram > 0 → Increasing momentum
- Histogram < 0 → Decreasing momentum

**Best For**:
- Trend confirmation
- Momentum shifts
- Entry/exit signals

**Tips**:
- Standard settings (12, 26, 9) work well for most assets
- Divergence between MACD and price can signal reversals
- Works better in trending markets

---

### **5. Bollinger Bands**

**Purpose**: Volatility indicator with upper and lower bands around a moving average.

**Formula**:
```
Middle Band = SMA(period)
Standard Deviation = sqrt(sum((price - SMA)²) / period)
Upper Band = Middle + (stdDev × multiplier)
Lower Band = Middle - (stdDev × multiplier)
Bandwidth = (Upper - Lower) / Middle
%B = (Price - Lower) / (Upper - Lower)
```

**Usage**:
```kotlin
// Single value
val bb = candles.bollingerBands(
    period = 20,
    stdDevMultiplier = 2.0
)

if (bb != null) {
    println("Upper: ${bb.upper}")
    println("Middle: ${bb.middle}")
    println("Lower: ${bb.lower}")
    println("Bandwidth: ${bb.bandwidth}")
    println("%B: ${bb.percentB}")
    
    val currentPrice = candles.last().close.toDouble()
    
    // Check conditions
    if (bb.isTouchingUpperBand(currentPrice)) {
        println("Price touching upper band - potential sell")
    }
    
    if (bb.isTouchingLowerBand(currentPrice)) {
        println("Price touching lower band - potential buy")
    }
    
    if (bb.isSqueeze(threshold = 0.05)) {
        println("BB Squeeze detected - breakout likely")
    }
}

// Convenience functions
if (candles.isBBSqueeze()) {
    println("Low volatility - breakout imminent")
}

if (candles.isTouchingUpperBB()) {
    println("Price near upper band")
}
```

**Parameters**:
- `period`: Number of periods for SMA (default: 20)
- `stdDevMultiplier`: Standard deviation multiplier (default: 2.0)

**Interpretation**:
- Price touching upper band → Overbought (potential sell)
- Price touching lower band → Oversold (potential buy)
- Narrow bands (squeeze) → Low volatility, breakout likely
- Wide bands → High volatility
- Price walking along band → Strong trend

**Best For**:
- Volatility measurement
- Overbought/oversold detection
- Breakout trading (squeeze)

**Tips**:
- Don't sell just because price touches upper band in uptrend
- Combine with other indicators for confirmation
- %B can be used for precise entry timing

---

## 🔧 **Advanced Features**

### **Validation**

```kotlin
// Validate data before calculation
val validationResult = IndicatorValidator.validateData(
    data = candles,
    minDataPoints = 20
)

if (validationResult.isValid()) {
    // Safe to calculate indicator
    val sma = candles.sma(20)
}

// Check for data quality issues
val outliers = IndicatorValidator.findSuspiciousVolumes(candles)
if (outliers.isNotEmpty()) {
    println("Warning: Found ${outliers.size} candles with suspicious volume")
}
```

### **Utility Functions**

```kotlin
// Calculate standard deviation
val stdDev = IndicatorUtils.calculateStandardDeviation(listOf(10.0, 20.0, 30.0))

// Detect crossovers
val crossover = IndicatorUtils.detectCrossover(
    current = ema12,
    previous = previousEma12,
    threshold = ema26,
    previousThreshold = previousEma26
)

when (crossover) {
    CrossoverType.BULLISH -> println("Bullish crossover")
    CrossoverType.BEARISH -> println("Bearish crossover")
    CrossoverType.NONE -> println("No crossover")
}

// Detect trend
val closes = candles.map { it.close.toDouble() }
val trend = IndicatorUtils.detectTrend(closes, minPeriod = 3)

when (trend) {
    TrendType.UPTREND -> println("Upward trend detected")
    TrendType.DOWNTREND -> println("Downward trend detected")
    TrendType.SIDEWAYS -> println("Sideways / consolidation")
}
```

---

## 📝 **Best Practices**

### **1. Combine Multiple Indicators**

```kotlin
// Example: Confirm trend with multiple indicators
fun isBullishSetup(candles: List<Candlestick>): Boolean {
    val sma50 = candles.sma(50) ?: return false
    val currentPrice = candles.last().close.toDouble()
    
    // 1. Price above SMA(50)
    val aboveSMA = currentPrice > sma50
    
    // 2. MACD bullish
    val macdBullish = candles.isMACDBullish()
    
    // 3. RSI not overbought
    val rsi = candles.rsi() ?: return false
    val rsiOK = !RSIIndicator.isOverbought(rsi)
    
    return aboveSMA && macdBullish && rsiOK
}
```

### **2. Use Appropriate Timeframes**

- Short-term trading: 1m, 5m, 15m candles with shorter periods
- Swing trading: 1h, 4h candles with medium periods
- Position trading: 1d candles with longer periods

### **3. Validate Data Quality**

```kotlin
// Always validate before important calculations
if (!IndicatorValidator.validateData(candles, minDataPoints = 50).isValid()) {
    logger.warn { "Insufficient or invalid data" }
    return null
}
```

### **4. Handle Null Results**

```kotlin
// Indicators return null when insufficient data
val rsi = candles.rsi(14)
if (rsi == null) {
    logger.warn { "Cannot calculate RSI - insufficient data" }
    return
}

// Or use safe calls
candles.rsi()?.let { rsi ->
    println("RSI: $rsi")
}
```

---

## ⚠️ **Common Pitfalls**

1. **Insufficient Data**
   - Ensure you have enough candlesticks for the indicator period
   - RSI needs period + 1 data points
   - MACD needs slowPeriod + signalPeriod - 1

2. **Lagging Indicators**
   - Moving averages and MACD lag behind price
   - Use for confirmation, not prediction

3. **False Signals in Ranging Markets**
   - MACD and moving averages work better in trends
   - RSI can give false signals in strong trends

4. **Overfitting**
   - Don't optimize parameters based on historical data
   - Standard parameters exist for a reason

5. **Ignoring Context**
   - Always consider overall market conditions
   - Volume confirmation is important

---

## 🎯 **Trading Strategy Examples**

### **Strategy 1: SMA Crossover**

```kotlin
fun smaGoldenCross(candles: List<Candlestick>): Boolean {
    val sma50All = candles.smaAll(50)
    val sma200All = candles.smaAll(200)
    
    if (sma50All.size < 2 || sma200All.size < 2) return false
    
    val current50 = sma50All.last() ?: return false
    val current200 = sma200All.last() ?: return false
    val prev50 = sma50All[sma50All.size - 2] ?: return false
    val prev200 = sma200All[sma200All.size - 2] ?: return false
    
    // Golden Cross: SMA(50) crosses above SMA(200)
    return prev50 <= prev200 && current50 > current200
}
```

### **Strategy 2: RSI Oversold Bounce**

```kotlin
fun rsiOversoldBounce(candles: List<Candlestick>): Boolean {
    val rsiAll = candles.rsiAll(14)
    
    if (rsiAll.size < 2) return false
    
    val currentRSI = rsiAll.last() ?: return false
    val prevRSI = rsiAll[rsiAll.size - 2] ?: return false
    
    // RSI was oversold, now bouncing back
    return prevRSI < 30.0 && currentRSI > 30.0
}
```

### **Strategy 3: Bollinger Band Squeeze Breakout**

```kotlin
fun bollingerSqueezeBreakout(candles: List<Candlestick>): String? {
    val bbAll = candles.bollingerBandsAll()
    
    if (bbAll.size < 2) return null
    
    val current = bbAll.last() ?: return null
    val previous = bbAll[bbAll.size - 2] ?: return null
    
    // Was squeezing, now breaking out
    if (previous.isSqueeze(0.05) && !current.isSqueeze(0.05)) {
        val currentPrice = candles.last().close.toDouble()
        
        return when {
            currentPrice > current.upper -> "LONG"  // Broke above
            currentPrice < current.lower -> "SHORT" // Broke below
            else -> null
        }
    }
    
    return null
}
```

---

## 🚀 **Performance Considerations**

- All indicators are optimized for real-time calculation
- Use `calculateAll()` for batch processing of historical data
- Extension functions provide the most convenient API
- Indicators are stateless except EMA (call `reset()` when switching symbols)

---

## 📚 **Additional Resources**

- **TradingView**: Verify calculations against industry standard
- **Investopedia**: Learn indicator interpretation
- **Issue #10**: Technical implementation details

---

**Created**: October 30, 2025  
**Module**: `core-service/indicators`  
**Maintainer**: FMPS AutoTrader Team

