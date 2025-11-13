package com.fmps.autotrader.desktop.services

import kotlinx.coroutines.flow.Flow

data class TelemetrySample(
    val channel: String,
    val payload: String
)

interface TelemetryClient {
    fun start()
    fun stop()
    fun samples(): Flow<TelemetrySample>
}


