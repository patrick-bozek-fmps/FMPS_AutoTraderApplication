package de.fmps.app_onprem

class AI_Trading_Agent(
    private val budget: Double,
    private val tradingResolution: String,  // z. B. "1m", "5m", "1h"
    private val tradingDurationMinutes: Int,
    private val targetRevenuePercent: Double,
    private val maxLeverage: Int,
    private val tradingAsset: String,
    private val connector: Connector_Bitget
) {
    private var activePosition: Position? = null
    private val indicators = TechnicalIndicators()

    fun startTrading() {
        println("AI Trading Agent started for asset $tradingAsset")
        val endTime = System.currentTimeMillis() + tradingDurationMinutes * 60_000

        while (System.currentTimeMillis() < endTime) {
            val candles = connector.getCandlestickData(tradingAsset, tradingResolution)
            val decision = analyzeMarket(candles)

            when (decision.action) {
                TradeAction.BUY -> openLong(decision.stopLoss)
                TradeAction.SELL -> openShort(decision.stopLoss)
                TradeAction.HOLD -> println("Holding position…")
                TradeAction.CLOSE -> closePosition()
            }

            // Stop-Loss dynamisch anpassen
            updateStopLoss(candles)

            Thread.sleep(1000 * 60) // alle 60 Sekunden analysieren
        }

        println("Trading session ended.")
    }

    private fun analyzeMarket(candles: List<Candlestick>): TradeDecision {
        val rsi = indicators.calculateRSI(candles)
        val macd = indicators.calculateMACD(candles)
        val smaShort = indicators.calculateSMA(candles, 9)
        val smaLong = indicators.calculateSMA(candles, 21)

        return when {
            rsi < 30 && smaShort > smaLong && macd.signal > macd.macd ->
                TradeDecision(TradeAction.BUY, stopLoss = candles.last().close * 0.98)
            rsi > 70 && smaShort < smaLong && macd.signal < macd.macd ->
                TradeDecision(TradeAction.SELL, stopLoss = candles.last().close * 1.02)
            else -> TradeDecision(TradeAction.HOLD)
        }
    }

    private fun openLong(stopLoss: Double) {
        if (activePosition == null) {
            activePosition = connector.openPosition(
                asset = tradingAsset,
                direction = "LONG",
                leverage = maxLeverage,
                budget = budget,
                stopLoss = stopLoss
            )
            println("Opened LONG position at stop-loss $stopLoss")
        }
    }

    private fun openShort(stopLoss: Double) {
        if (activePosition == null) {
            activePosition = connector.openPosition(
                asset = tradingAsset,
                direction = "SHORT",
                leverage = maxLeverage,
                budget = budget,
                stopLoss = stopLoss
            )
            println("Opened SHORT position at stop-loss $stopLoss")
        }
    }

    private fun closePosition() {
        activePosition?.let {
            connector.closePosition(it)
            println("Closed position.")
            activePosition = null
        }
    }

    private fun updateStopLoss(candles: List<Candlestick>) {
        activePosition?.let {
            val newStop = when (it.direction) {
                "LONG" -> candles.last().close * 0.98
                "SHORT" -> candles.last().close * 1.02
                else -> it.stopLoss
            }
            if (Math.abs(newStop - it.stopLoss) / it.stopLoss > 0.002) {
                connector.modifyStopLoss(it, newStop)
                println("Stop-Loss updated to $newStop")
            }
        }
    }
}

class Connector_Bitget {
    fun getCandlestickData(asset: String, interval: String): List<Candlestick> {
        // Hier API-Aufruf oder lokale Simulation implementieren
        return listOf()
    }

    fun openPosition(asset: String, direction: String, leverage: Int, budget: Double, stopLoss: Double): Position {
        // Bitget API Call: Order öffnen
        return Position(asset, direction, leverage, budget, stopLoss)
    }

    fun modifyStopLoss(position: Position, newStopLoss: Double) {
        // API Call zum Update
    }

    fun closePosition(position: Position) {
        // API Call zum Schließen
    }
}

class TechnicalIndicators {
    fun calculateSMA(candles: List<Candlestick>, period: Int): Double {
        if (candles.size < period) return 0.0
        return candles.takeLast(period).map { it.close }.average()
    }

    fun calculateRSI(candles: List<Candlestick>, period: Int = 14): Double {
        if (candles.size <= period) return 50.0
        val changes = candles.zipWithNext { a, b -> b.close - a.close }
        val gains = changes.filter { it > 0 }.sum() / period
        val losses = -changes.filter { it < 0 }.sum() / period
        val rs = if (losses == 0.0) return 100.0 else gains / losses
        return 100 - (100 / (1 + rs))
    }

    fun calculateMACD(candles: List<Candlestick>): MACD {
        val ema12 = ema(candles, 12)
        val ema26 = ema(candles, 26)
        val macd = ema12 - ema26
        val signal = ema(candles, 9, macd)
        return MACD(macd, signal)
    }

    private fun ema(candles: List<Candlestick>, period: Int, baseValue: Double? = null): Double {
        val prices = candles.map { it.close }
        val k = 2.0 / (period + 1)
        return prices.reduce { acc, price -> (price * k) + (acc * (1 - k)) }
    }
}

data class MACD(val macd: Double, val signal: Double)
class TechnicalIndicators {
    fun calculateSMA(candles: List<Candlestick>, period: Int): Double {
        if (candles.size < period) return 0.0
        return candles.takeLast(period).map { it.close }.average()
    }

    fun calculateRSI(candles: List<Candlestick>, period: Int = 14): Double {
        if (candles.size <= period) return 50.0
        val changes = candles.zipWithNext { a, b -> b.close - a.close }
        val gains = changes.filter { it > 0 }.sum() / period
        val losses = -changes.filter { it < 0 }.sum() / period
        val rs = if (losses == 0.0) return 100.0 else gains / losses
        return 100 - (100 / (1 + rs))
    }

    fun calculateMACD(candles: List<Candlestick>): MACD {
        val ema12 = ema(candles, 12)
        val ema26 = ema(candles, 26)
        val macd = ema12 - ema26
        val signal = ema(candles, 9, macd)
        return MACD(macd, signal)
    }

    private fun ema(candles: List<Candlestick>, period: Int, baseValue: Double? = null): Double {
        val prices = candles.map { it.close }
        val k = 2.0 / (period + 1)
        return prices.reduce { acc, price -> (price * k) + (acc * (1 - k)) }
    }
}

data class MACD(val macd: Double, val signal: Double)


}


