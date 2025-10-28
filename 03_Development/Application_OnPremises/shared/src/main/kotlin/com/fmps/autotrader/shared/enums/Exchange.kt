package com.fmps.autotrader.shared.enums

import kotlinx.serialization.Serializable

/**
 * Supported cryptocurrency exchanges.
 */
@Serializable
enum class Exchange {
    BINANCE,
    BITGET,
    KRAKEN,
    COINBASE,
    KUCOIN
}

