package com.fmps.autotrader.core.database.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Database table for system and trader configurations
 * 
 * Key-value store for flexible configuration management
 */
object ConfigurationsTable : Table("configurations") {
    val id = integer("id").autoIncrement()
    val key = varchar("key", 100).uniqueIndex()
    val value = text("value")
    val category = varchar("category", 50) // SYSTEM, TRADER, EXCHANGE, UI
    val dataType = varchar("data_type", 20) // STRING, INTEGER, DECIMAL, BOOLEAN, JSON
    val description = text("description").nullable()
    val isEncrypted = bool("is_encrypted").default(false)
    val isEditable = bool("is_editable").default(true)
    
    // Timestamps
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    
    override val primaryKey = PrimaryKey(id)
}

