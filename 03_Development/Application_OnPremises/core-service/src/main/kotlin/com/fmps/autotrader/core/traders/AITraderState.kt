package com.fmps.autotrader.core.traders

/**
 * Internal state of an AI Trader instance.
 *
 * This enum represents the lifecycle states of an AI Trader during its operation.
 * State transitions must follow the defined lifecycle:
 *
 * ```
 * IDLE → STARTING → RUNNING → STOPPING → STOPPED
 *   ↓       ↓          ↓
 *   └───────┴──────────┴→ ERROR
 *              ↓
 *            PAUSED → RUNNING (via resume)
 * ```
 *
 * @since 1.0.0
 */
enum class AITraderState {
    /**
     * Trader is idle (initial state, or after being stopped).
     * No trading activity is occurring.
     */
    IDLE,

    /**
     * Trader is starting up.
     * Initializing connections, loading configuration, setting up strategies.
     */
    STARTING,

    /**
     * Trader is actively running.
     * Processing market data, generating signals, executing trades.
     */
    RUNNING,

    /**
     * Trader is paused.
     * Temporarily stopped from trading but maintains state and can be resumed.
     */
    PAUSED,

    /**
     * Trader is stopping.
     * Gracefully shutting down, closing positions, saving state.
     */
    STOPPING,

    /**
     * Trader is stopped.
     * Shutdown complete, no longer active.
     */
    STOPPED,

    /**
     * Trader encountered an error.
     * Requires intervention to recover.
     */
    ERROR
}

