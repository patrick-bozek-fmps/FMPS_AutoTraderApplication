package com.fmps.autotrader.core.database.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Database table for exchange account credentials
 * 
 * Stores encrypted API keys and account information
 * Demo/Testnet accounts only for v1.0
 */
object ExchangeAccountsTable : Table("exchange_accounts") {
    val id = integer("id").autoIncrement()
    val exchangeName = varchar("exchange_name", 20) // BINANCE, BITGET, TRADINGVIEW
    val accountName = varchar("account_name", 100)
    val accountType = varchar("account_type", 20) // DEMO, TESTNET (no REAL in v1.0)
    
    // Encrypted Credentials (AES-256)
    val apiKey = text("api_key") // Encrypted
    val apiSecret = text("api_secret") // Encrypted
    val passphrase = text("passphrase").nullable() // Encrypted (for Bitget)
    val encryptionIv = varchar("encryption_iv", 64) // Initialization vector for decryption
    
    // Account Status
    val isActive = bool("is_active").default(true)
    val isDefault = bool("is_default").default(false)
    val lastConnectionTest = datetime("last_connection_test").nullable()
    val connectionStatus = varchar("connection_status", 20).nullable() // SUCCESS, FAILED, UNKNOWN
    val lastErrorMessage = text("last_error_message").nullable()
    
    // Balance Tracking (demo/virtual)
    val initialBalance = decimal("initial_balance", 18, 8).nullable()
    val currentBalance = decimal("current_balance", 18, 8).nullable()
    val currency = varchar("currency", 10).default("USDT")
    
    // Timestamps
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    
    override val primaryKey = PrimaryKey(id)
}

