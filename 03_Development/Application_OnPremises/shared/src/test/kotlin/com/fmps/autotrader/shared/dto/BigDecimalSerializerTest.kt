package com.fmps.autotrader.shared.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Tests for BigDecimal Serialization
 */
class BigDecimalSerializerTest {
    
    private val json = Json { prettyPrint = false }
    
    @Test
    fun `should serialize BigDecimal to JSON string`() {
        val value = BigDecimal("123.456789")
        val serialized = json.encodeToString(BigDecimalSerializer, value)
        
        assertEquals("\"123.456789\"", serialized)
    }
    
    @Test
    fun `should deserialize JSON string to BigDecimal`() {
        val jsonString = "\"123.456789\""
        val deserialized = json.decodeFromString(BigDecimalSerializer, jsonString)
        
        assertEquals(BigDecimal("123.456789"), deserialized)
    }
    
    @Test
    fun `should handle BigDecimal with high precision`() {
        val value = BigDecimal("0.00000001")
        val serialized = json.encodeToString(BigDecimalSerializer, value)
        val deserialized = json.decodeFromString(BigDecimalSerializer, serialized)
        
        assertEquals(value, deserialized)
    }
    
    @Test
    fun `should handle large BigDecimal values`() {
        val value = BigDecimal("999999999.99999999")
        val serialized = json.encodeToString(BigDecimalSerializer, value)
        val deserialized = json.decodeFromString(BigDecimalSerializer, serialized)
        
        assertEquals(value, deserialized)
    }
    
    @Test
    fun `should handle negative BigDecimal values`() {
        val value = BigDecimal("-123.456")
        val serialized = json.encodeToString(BigDecimalSerializer, value)
        val deserialized = json.decodeFromString(BigDecimalSerializer, serialized)
        
        assertEquals(value, deserialized)
    }
    
    @Test
    fun `should handle zero BigDecimal value`() {
        val value = BigDecimal.ZERO
        val serialized = json.encodeToString(BigDecimalSerializer, value)
        val deserialized = json.decodeFromString(BigDecimalSerializer, serialized)
        
        assertEquals(value, deserialized)
    }
    
    @Test
    fun `should preserve precision in round-trip`() {
        val value = BigDecimal("1234.567890123456789")
        val serialized = json.encodeToString(BigDecimalSerializer, value)
        val deserialized = json.decodeFromString(BigDecimalSerializer, serialized)
        
        // Compare as strings to ensure exact precision
        assertEquals(value.toPlainString(), deserialized.toPlainString())
    }
}

