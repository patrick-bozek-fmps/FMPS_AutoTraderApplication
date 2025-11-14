package com.fmps.autotrader.desktop.monitoring

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.Candlestick
import com.fmps.autotrader.desktop.services.OpenPosition
import com.fmps.autotrader.desktop.services.Timeframe
import com.fmps.autotrader.desktop.services.TradeRecord

data class MonitoringState(
    val isLoading: Boolean = true,
    val timeframe: Timeframe = Timeframe.FIVE_MIN,
    val candles: List<Candlestick> = emptyList(),
    val positions: List<OpenPosition> = emptyList(),
    val trades: List<TradeRecord> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED
)

enum class ConnectionStatus {
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}

sealed interface MonitoringEvent : ViewEvent {
    data class ShowMessage(val message: String) : MonitoringEvent
}

