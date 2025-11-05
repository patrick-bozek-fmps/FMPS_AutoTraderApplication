package com.fmps.autotrader.core.traders

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.TradingStrategy
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Duration

/**
 * Configuration for an AI Trader instance.
 *
 * This data class contains all configuration parameters required to create and operate
 * an AI Trader, as specified in ATP_ProdSpec_53.
 *
 * @property id Unique identifier for the trader
 * @property name Human-readable name for the trader
 * @property exchange Exchange to trade on (BINANCE, BITGET)
 * @property symbol Trading pair symbol (e.g., "BTCUSDT")
 * @property virtualMoney Whether to use virtual money (v1.0: always true)
 * @property maxStakeAmount Maximum amount of money to stake per trade
 * @property maxRiskLevel Maximum risk/leverage level (1-10)
 * @property maxTradingDuration Maximum trading duration before auto-stop
 * @property minReturnPercent Minimum return/profit target percentage
 * @property strategy Trading strategy to use (TREND_FOLLOWING, MEAN_REVERSION, BREAKOUT)
 * @property candlestickInterval Time frame for candlestick data (1m, 5m, 1h, etc.)
 *
 * @since 1.0.0
 */
@Serializable
data class AITraderConfig(
    val id: String,
    val name: String,
    val exchange: Exchange,
    val symbol: String,
    val virtualMoney: Boolean = true, // v1.0: always true (demo only)
    @Serializable(with = BigDecimalSerializer::class)
    val maxStakeAmount: BigDecimal,
    val maxRiskLevel: Int, // 1-10
    @Contextual val maxTradingDuration: Duration,
    val minReturnPercent: Double,
    val strategy: TradingStrategy,
    val candlestickInterval: TimeFrame
) {
    init {
        require(id.isNotBlank()) { "ID cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(symbol.isNotBlank()) { "Symbol cannot be blank" }
        require(maxStakeAmount > BigDecimal.ZERO) {
            "Max stake amount must be positive, got: $maxStakeAmount"
        }
        require(maxRiskLevel in 1..10) {
            "Max risk level must be between 1 and 10, got: $maxRiskLevel"
        }
        require(!maxTradingDuration.isNegative && !maxTradingDuration.isZero) {
            "Max trading duration must be positive"
        }
        require(minReturnPercent >= 0.0) {
            "Min return percent must be non-negative, got: $minReturnPercent"
        }
    }

}

