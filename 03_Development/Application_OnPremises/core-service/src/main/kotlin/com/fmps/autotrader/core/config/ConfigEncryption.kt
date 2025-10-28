package com.fmps.autotrader.core.config

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for encrypting and decrypting sensitive configuration values (API keys, secrets).
 * 
 * Uses AES-256-GCM encryption with a master key that must be provided via environment variable.
 * 
 * Usage:
 * ```kotlin
 * // Encrypt a value
 * val encrypted = ConfigEncryption.encrypt("my-secret-api-key")
 * 
 * // Decrypt a value
 * val decrypted = ConfigEncryption.decrypt(encrypted)
 * ```
 * 
 * Master Key Management:
 * - Set FMPS_MASTER_KEY environment variable with a base64-encoded 256-bit key
 * - Generate a key: `ConfigEncryption.generateMasterKey()`
 * - Store securely (never commit to git!)
 */
object ConfigEncryption {
    
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12  // 96 bits for GCM
    private const val TAG_SIZE = 128  // 128 bits authentication tag
    private const val MASTER_KEY_ENV = "FMPS_MASTER_KEY"
    
    private val masterKey: SecretKey by lazy {
        loadMasterKey()
    }
    
    /**
     * Encrypts a plaintext string using AES-256-GCM.
     * 
     * @param plaintext The text to encrypt
     * @return Base64-encoded encrypted data (includes IV and ciphertext)
     * @throws ConfigurationException if encryption fails or master key is not configured
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) {
            throw ConfigurationException("Cannot encrypt blank text")
        }
        
        try {
            // Generate random IV
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            
            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmSpec)
            
            // Encrypt
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV and ciphertext
            val combined = ByteBuffer.allocate(IV_SIZE + ciphertext.size)
            combined.put(iv)
            combined.put(ciphertext)
            
            // Encode to Base64
            return Base64.getEncoder().encodeToString(combined.array())
        } catch (e: Exception) {
            throw ConfigurationException("Encryption failed: ${e.message}", e)
        }
    }
    
    /**
     * Decrypts a Base64-encoded encrypted string.
     * 
     * @param encrypted Base64-encoded encrypted data
     * @return Decrypted plaintext
     * @throws ConfigurationException if decryption fails or master key is not configured
     */
    fun decrypt(encrypted: String): String {
        if (encrypted.isBlank()) {
            throw ConfigurationException("Cannot decrypt blank text")
        }
        
        try {
            // Decode from Base64
            val combined = Base64.getDecoder().decode(encrypted)
            
            // Extract IV and ciphertext
            val buffer = ByteBuffer.wrap(combined)
            val iv = ByteArray(IV_SIZE)
            buffer.get(iv)
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)
            
            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmSpec)
            
            // Decrypt
            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            throw ConfigurationException("Decryption failed: ${e.message}", e)
        }
    }
    
    /**
     * Checks if a string appears to be encrypted (base64 format).
     */
    fun isEncrypted(value: String): Boolean {
        if (value.isBlank()) return false
        
        return try {
            val decoded = Base64.getDecoder().decode(value)
            // Check if it has at least IV + some ciphertext
            decoded.size > IV_SIZE
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Safely decrypts a value if it's encrypted, otherwise returns the original value.
     * 
     * @param value The value to decrypt if needed
     * @return Decrypted value if encrypted, or original value if not
     */
    fun decryptIfNeeded(value: String): String {
        return if (isEncrypted(value)) {
            try {
                decrypt(value)
            } catch (e: Exception) {
                // If decryption fails, assume it's not encrypted
                value
            }
        } else {
            value
        }
    }
    
    /**
     * Generates a new random master key for encryption.
     * 
     * @return Base64-encoded master key (store this securely!)
     */
    fun generateMasterKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE, SecureRandom())
        val key = keyGen.generateKey()
        return Base64.getEncoder().encodeToString(key.encoded)
    }
    
    /**
     * Checks if a master key is configured.
     */
    fun isMasterKeyConfigured(): Boolean {
        return System.getenv(MASTER_KEY_ENV) != null
    }
    
    /**
     * Loads the master key from environment variable.
     */
    private fun loadMasterKey(): SecretKey {
        val keyBase64 = System.getenv(MASTER_KEY_ENV)
            ?: throw ConfigurationException(
                "Master key not configured. Set $MASTER_KEY_ENV environment variable with a base64-encoded 256-bit key. " +
                "Generate one with: ConfigEncryption.generateMasterKey()"
            )
        
        return try {
            val keyBytes = Base64.getDecoder().decode(keyBase64)
            if (keyBytes.size != KEY_SIZE / 8) {
                throw ConfigurationException(
                    "Invalid master key size: ${keyBytes.size * 8} bits (expected $KEY_SIZE bits)"
                )
            }
            SecretKeySpec(keyBytes, "AES")
        } catch (e: IllegalArgumentException) {
            throw ConfigurationException("Invalid master key format (must be valid Base64)", e)
        }
    }
}

/**
 * Extension function to decrypt API credentials if encryption is enabled.
 * 
 * Example:
 * ```kotlin
 * val apiConfig = config.exchanges.binance?.api?.decrypted()
 * ```
 */
fun ApiConfig.decrypted(): ApiConfig {
    if (!ConfigEncryption.isMasterKeyConfigured()) {
        // If no master key, assume values are not encrypted
        return this
    }
    
    return this.copy(
        key = ConfigEncryption.decryptIfNeeded(this.key),
        secret = ConfigEncryption.decryptIfNeeded(this.secret),
        passphrase = this.passphrase?.let { ConfigEncryption.decryptIfNeeded(it) }
    )
}

