package com.fmps.autotrader.core.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@DisplayName("ConfigEncryption Tests")
class ConfigEncryptionTest {
    
    @Nested
    @DisplayName("Master Key Generation")
    inner class MasterKeyGeneration {
        
        @Test
        fun `should generate valid master key`() {
            // When
            val masterKey = ConfigEncryption.generateMasterKey()
            
            // Then
            masterKey.shouldNotBeBlank()
            masterKey.length shouldBe 44  // Base64 encoding of 32 bytes (256 bits)
        }
        
        @Test
        fun `should generate different master keys each time`() {
            // When
            val key1 = ConfigEncryption.generateMasterKey()
            val key2 = ConfigEncryption.generateMasterKey()
            
            // Then
            key1 shouldNotBe key2
        }
    }
    
    @Nested
    @DisplayName("Encryption and Decryption")
    @EnabledIfEnvironmentVariable(named = "FMPS_MASTER_KEY", matches = ".+")
    inner class EncryptionAndDecryption {
        
        @Test
        fun `should encrypt and decrypt simple text`() {
            // Given
            val plaintext = "my-secret-api-key"
            
            // When
            val encrypted = ConfigEncryption.encrypt(plaintext)
            val decrypted = ConfigEncryption.decrypt(encrypted)
            
            // Then
            encrypted shouldNotBe plaintext
            decrypted shouldBe plaintext
        }
        
        @Test
        fun `should encrypt and decrypt complex text`() {
            // Given
            val plaintext = "My\$ecr3t!Key@2024#with%Special^Chars&and*(Numbers)123"
            
            // When
            val encrypted = ConfigEncryption.encrypt(plaintext)
            val decrypted = ConfigEncryption.decrypt(encrypted)
            
            // Then
            encrypted shouldNotBe plaintext
            decrypted shouldBe plaintext
        }
        
        @Test
        fun `should produce different ciphertexts for same plaintext`() {
            // Given
            val plaintext = "test-secret"
            
            // When
            val encrypted1 = ConfigEncryption.encrypt(plaintext)
            val encrypted2 = ConfigEncryption.encrypt(plaintext)
            
            // Then
            encrypted1 shouldNotBe encrypted2  // Different IVs
            ConfigEncryption.decrypt(encrypted1) shouldBe plaintext
            ConfigEncryption.decrypt(encrypted2) shouldBe plaintext
        }
        
        @Test
        fun `should throw exception for blank plaintext`() {
            // When & Then
            shouldThrow<ConfigurationException> {
                ConfigEncryption.encrypt("")
            }
        }
        
        @Test
        fun `should throw exception for blank ciphertext`() {
            // When & Then
            shouldThrow<ConfigurationException> {
                ConfigEncryption.decrypt("")
            }
        }
        
        @Test
        fun `should throw exception for invalid ciphertext`() {
            // Given
            val invalidCiphertext = "not-a-valid-encrypted-string"
            
            // When & Then
            shouldThrow<ConfigurationException> {
                ConfigEncryption.decrypt(invalidCiphertext)
            }
        }
    }
    
    @Nested
    @DisplayName("Encrypted Value Detection")
    inner class EncryptedValueDetection {
        
        @Test
        fun `should detect if value is encrypted`() {
            // Given
            val plaintext = "not-encrypted"
            
            // When & Then
            ConfigEncryption.isEncrypted(plaintext) shouldBe false
        }
        
        @Test
        fun `should detect blank values as not encrypted`() {
            // When & Then
            ConfigEncryption.isEncrypted("") shouldBe false
            ConfigEncryption.isEncrypted("   ") shouldBe false
        }
    }
    
    @Nested
    @DisplayName("Conditional Decryption")
    inner class ConditionalDecryption {
        
        @Test
        fun `should return plaintext unchanged if not encrypted`() {
            // Given
            val plaintext = "not-encrypted-value"
            
            // When
            val result = ConfigEncryption.decryptIfNeeded(plaintext)
            
            // Then
            result shouldBe plaintext
        }
    }
    
    @Nested
    @DisplayName("API Config Extension")
    @EnabledIfEnvironmentVariable(named = "FMPS_MASTER_KEY", matches = ".+")
    inner class ApiConfigExtension {
        
        @Test
        fun `should decrypt API config credentials`() {
            // Given
            val encryptedKey = ConfigEncryption.encrypt("test-api-key")
            val encryptedSecret = ConfigEncryption.encrypt("test-api-secret")
            
            val apiConfig = ApiConfig(
                key = encryptedKey,
                secret = encryptedSecret,
                passphrase = null,
                baseUrl = "https://api.example.com",
                websocketUrl = "wss://ws.example.com"
            )
            
            // When
            val decrypted = apiConfig.decrypted()
            
            // Then
            decrypted.key shouldBe "test-api-key"
            decrypted.secret shouldBe "test-api-secret"
        }
        
        @Test
        fun `should handle plain API config without master key`() {
            // Given
            val apiConfig = ApiConfig(
                key = "plain-api-key",
                secret = "plain-secret",
                passphrase = null,
                baseUrl = "https://api.example.com",
                websocketUrl = "wss://ws.example.com"
            )
            
            // When
            val result = apiConfig.decrypted()
            
            // Then - should return as-is if not encrypted
            result.key shouldBe "plain-api-key"
            result.secret shouldBe "plain-secret"
        }
    }
    
    @Nested
    @DisplayName("Master Key Configuration")
    inner class MasterKeyConfiguration {
        
        @Test
        fun `should detect if master key is configured`() {
            // When
            val isConfigured = ConfigEncryption.isMasterKeyConfigured()
            
            // Then
            // Result depends on whether FMPS_MASTER_KEY env var is set
            // In CI/testing, this might be false
            isConfigured shouldBe (System.getenv("FMPS_MASTER_KEY") != null)
        }
    }
}

