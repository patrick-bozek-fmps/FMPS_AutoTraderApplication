package com.fmps.autotrader.core.traders

/**
 * Action represented by a trading signal.
 *
 * This enum defines the possible actions that a trading signal can recommend.
 * It is separate from [com.fmps.autotrader.shared.enums.TradeAction] which represents
 * position sides (LONG/SHORT). Signal actions represent trading decisions.
 *
 * @since 1.0.0
 */
enum class SignalAction {
    /**
     * Buy signal - Open a long position.
     * Maps to TradeAction.LONG when executing trades.
     */
    BUY,

    /**
     * Sell signal - Open a short position.
     * Maps to TradeAction.SHORT when executing trades.
     */
    SELL,

    /**
     * Hold signal - No action should be taken.
     * Maintain current position or remain flat.
     */
    HOLD,

    /**
     * Close signal - Close the current position.
     * Exit any open positions regardless of direction.
     */
    CLOSE
}

