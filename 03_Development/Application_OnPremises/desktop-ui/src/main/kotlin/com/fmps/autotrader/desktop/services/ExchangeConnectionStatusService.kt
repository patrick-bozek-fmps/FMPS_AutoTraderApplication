package com.fmps.autotrader.desktop.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple service to track exchange connection status across the application.
 * Updated when connection tests are performed in Configuration tab.
 */
class ExchangeConnectionStatusService {
    private val _binanceStatus = MutableStateFlow<Boolean?>(null) // null = not tested, true = connected, false = failed
    val binanceStatus: StateFlow<Boolean?> = _binanceStatus.asStateFlow()
    
    private val _bitgetStatus = MutableStateFlow<Boolean?>(null) // null = not tested, true = connected, false = failed
    val bitgetStatus: StateFlow<Boolean?> = _bitgetStatus.asStateFlow()
    
    fun updateBinanceStatus(connected: Boolean) {
        println("🔍 ExchangeConnectionStatusService: Updating Binance status to $connected")
        _binanceStatus.value = connected
    }
    
    fun updateBitgetStatus(connected: Boolean) {
        println("🔍 ExchangeConnectionStatusService: Updating Bitget status to $connected")
        _bitgetStatus.value = connected
    }
}

