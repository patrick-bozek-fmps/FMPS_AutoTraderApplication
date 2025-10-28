package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.enums.Exchange
import kotlinx.serialization.Serializable

/**
 * Configuration for an exchange connector.
 */
@Serializable
data class ExchangeConfig(
    val exchange: Exchange,
    val apiKey: String,
    val apiSecret: String,
    val passphrase: String? = null,
    val baseUrl: String? = null,
    val testnet: Boolean = true
)

