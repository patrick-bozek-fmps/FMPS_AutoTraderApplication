package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents a price level in the order book.
 *
 * @param price The price level
 * @param quantity The quantity at this price level
 */
@Serializable
data class OrderBookEntry(
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal
)

/**
 * Represents the order book (market depth) for a trading pair.
 *
 * @param symbol The trading pair symbol (e.g., "BTCUSDT")
 * @param bids List of bid entries (buy orders), sorted by price descending
 * @param asks List of ask entries (sell orders), sorted by price ascending
 * @param timestamp Order book timestamp
 */
@Serializable
data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Instant.now()
) {
    /**
     * Gets the best bid (highest buy price).
     */
    fun getBestBid(): OrderBookEntry? = bids.firstOrNull()

    /**
     * Gets the best ask (lowest sell price).
     */
    fun getBestAsk(): OrderBookEntry? = asks.firstOrNull()

    /**
     * Gets the spread (difference between best ask and best bid).
     */
    fun getSpread(): BigDecimal? {
        val bid = getBestBid()?.price
        val ask = getBestAsk()?.price
        return if (bid != null && ask != null) ask - bid else null
    }

    /**
     * Gets the mid price (average of best bid and best ask).
     */
    fun getMidPrice(): BigDecimal? {
        val bid = getBestBid()?.price
        val ask = getBestAsk()?.price
        return if (bid != null && ask != null) (bid + ask) / BigDecimal(2) else null
    }
}

