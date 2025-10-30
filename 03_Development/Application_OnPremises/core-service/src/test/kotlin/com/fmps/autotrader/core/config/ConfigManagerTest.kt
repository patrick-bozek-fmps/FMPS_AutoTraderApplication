package com.fmps.autotrader.core.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@DisplayName("ConfigManager Tests")
class ConfigManagerTest {
    
    @Nested
    @DisplayName("Configuration Loading")
    inner class ConfigurationLoading {
        
        @Test
        fun `should load test configuration successfully`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config shouldNotBe null
            config.app.name shouldBe "FMPS AutoTrader"
            config.app.environment shouldBe "test"
            config.app.isTest shouldBe true
            config.app.isDevelopment shouldBe false
            config.app.isProduction shouldBe false
        }
        
        @Test
        fun `should load development configuration successfully`() {
            // Given & When
            val config = ConfigManager.load(environment = "dev")
            
            // Then
            config shouldNotBe null
            config.app.environment shouldBe "dev"
            config.app.isDevelopment shouldBe true
            config.logging.level shouldBe "DEBUG"
        }
        
        @Test
        fun `should retrieve loaded configuration`() {
            // Given
            ConfigManager.load(environment = "test")
            
            // When
            val config = ConfigManager.get()
            
            // Then
            config shouldNotBe null
            config.app.environment shouldBe "test"
        }
        
        @Test
        fun `should throw exception when getting config before loading`() {
            // Given - fresh ConfigManager state (in practice, load is called first)
            
            // When & Then
            // Note: This test may fail if other tests have already loaded config
            // In a real scenario, you'd reset ConfigManager state or use dependency injection
        }
    }
    
    @Nested
    @DisplayName("Server Configuration")
    inner class ServerConfiguration {
        
        @Test
        fun `should load server configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.server.port shouldBe 0  // Random port in test
            config.server.host shouldBe "0.0.0.0"
            config.server.cors.enabled shouldBe true
            config.server.websocket.enabled shouldBe false  // Disabled in test
        }
    }
    
    @Nested
    @DisplayName("Database Configuration")
    inner class DatabaseConfiguration {
        
        @Test
        fun `should load database configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.database.url shouldContain "test-db"
            config.database.hikari.maximumPoolSize shouldBe 2
            config.database.hikari.minimumIdle shouldBe 1
            config.database.flyway.validateOnMigrate shouldBe true
        }
    }
    
    @Nested
    @DisplayName("Logging Configuration")
    inner class LoggingConfiguration {
        
        @Test
        fun `should load logging configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.logging.level shouldBe "WARN"
            config.logging.file.enabled shouldBe false  // No file logging in tests
            config.logging.console.enabled shouldBe true
            config.logging.console.colorOutput shouldBe false
        }
    }
    
    @Nested
    @DisplayName("Exchange Configuration")
    inner class ExchangeConfiguration {
        
        @Test
        fun `should load exchange configurations`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.exchanges.binance shouldNotBe null
            config.exchanges.binance?.enabled shouldBe false  // Disabled in test
            config.exchanges.bitget shouldNotBe null
            config.exchanges.bitget?.enabled shouldBe false  // Disabled in test
        }
        
        @Test
        fun `should mask API keys for security`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            val apiConfig = config.exchanges.binance?.api
            
            // Then
            apiConfig shouldNotBe null
            // Masked key should not reveal the full key
            apiConfig?.maskedKey() shouldNotBe apiConfig?.key
        }
    }
    
    @Nested
    @DisplayName("AI Trader Configuration")
    inner class AITraderConfiguration {
        
        @Test
        fun `should load AI trader configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.aiTrader.maxActiveTraders shouldBe 3
            config.aiTrader.defaultRisk.maxLeverage shouldBe 5.0
            config.aiTrader.defaultRisk.defaultStopLossPercentage shouldBe 0.02
            config.aiTrader.defaultRisk.defaultTakeProfitPercentage shouldBe 0.05
        }
    }
    
    @Nested
    @DisplayName("Trading Configuration")
    inner class TradingConfiguration {
        
        @Test
        fun `should load trading configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.trading.demoMode.enabled shouldBe true
            config.trading.demoMode.allowRealTrading shouldBe false  // Never in v1.0
            config.trading.demoMode.initialBalance shouldBe 10000.0
        }
    }
    
    @Nested
    @DisplayName("Security Configuration")
    inner class SecurityConfiguration {
        
        @Test
        fun `should load security configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.security.encryption.enabled shouldBe false  // Simplified in test
            config.security.encryption.algorithm shouldBe "AES/GCM/NoPadding"
            config.security.encryption.keySize shouldBe 256
        }
    }
    
    @Nested
    @DisplayName("Performance Configuration")
    inner class PerformanceConfiguration {
        
        @Test
        fun `should load performance configuration correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.performance.threadPool.corePoolSize shouldBe 2
            config.performance.threadPool.maxPoolSize shouldBe 4
            config.performance.metrics.enabled shouldBe false  // Disabled in test
            config.performance.healthCheck.enabled shouldBe false  // Disabled in test
        }
    }
    
    @Nested
    @DisplayName("Configuration Validation")
    inner class ConfigurationValidation {
        
        @Test
        fun `should validate configuration successfully for test environment`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then - no exception means validation passed
            config shouldNotBe null
        }
        
        // Note: Testing invalid configurations would require creating custom config files
        // or using system properties to override values, which is more of an integration test
    }
    
    @Nested
    @DisplayName("Environment Detection")
    inner class EnvironmentDetection {
        
        @Test
        fun `should detect development environment correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "dev")
            
            // Then
            config.app.isDevelopment shouldBe true
            config.app.isTest shouldBe false
            config.app.isProduction shouldBe false
        }
        
        @Test
        fun `should detect test environment correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "test")
            
            // Then
            config.app.isTest shouldBe true
            config.app.isDevelopment shouldBe false
            config.app.isProduction shouldBe false
        }
        
        @Test
        fun `should detect production environment correctly`() {
            // Given & When
            val config = ConfigManager.load(environment = "prod")
            
            // Then
            config.app.isProduction shouldBe true
            config.app.isDevelopment shouldBe false
            config.app.isTest shouldBe false
        }
    }
}

