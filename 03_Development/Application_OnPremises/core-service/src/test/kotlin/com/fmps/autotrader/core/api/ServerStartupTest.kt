package com.fmps.autotrader.core.api

import com.fmps.autotrader.core.database.DatabaseFactory
import com.typesafe.config.ConfigFactory
import io.ktor.server.engine.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Test that the API server can start and stop successfully
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerStartupTest {
    
    private val testDbFile = File("build/test-db/server-startup-test.db")
    private var server: ApplicationEngine? = null
    
    @BeforeAll
    fun setup() {
        // Setup test database
        testDbFile.parentFile?.mkdirs()
        if (testDbFile.exists()) {
            testDbFile.delete()
        }
        
        val testConfig = ConfigFactory.parseString("""
            database {
                driver = "org.sqlite.JDBC"
                url = "jdbc:sqlite:${testDbFile.absolutePath.replace("\\", "/")}"
                hikari {
                    maximumPoolSize = 3
                    minimumIdle = 1
                    idleTimeout = 600000
                    connectionTimeout = 30000
                    maxLifetime = 1800000
                    autoCommit = true
                    poolName = "TestPool"
                }
                flyway {
                    baselineOnMigrate = true
                    baselineVersion = "0"
                    locations = [ "classpath:db/migration" ]
                    validateOnMigrate = true
                    cleanDisabled = true
                }
            }
        """)
        
        DatabaseFactory.init(testConfig)
    }
    
    @AfterAll
    fun tearDown() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
        DatabaseFactory.close()
        testDbFile.delete()
    }
    
    @Test
    fun `should start server on available port`() {
        // Given a random available port
        val testPort = 18080
        
        // When starting the server
        server = startApiServer(host = "127.0.0.1", port = testPort, wait = false)
        
        // Then server should not be null
        assertNotNull(server)
        
        // Give it a moment to start
        Thread.sleep(1000)
        
        // Then we can stop it
        server?.stop(gracePeriodMillis = 100, timeoutMillis = 1000)
    }
    
    @Test
    fun `should configure all plugins`() {
        // This test verifies that module configuration doesn't throw exceptions
        val testPort = 18081
        
        server = startApiServer(host = "127.0.0.1", port = testPort, wait = false)
        
        assertNotNull(server)
        
        Thread.sleep(1000)
        
        server?.stop(gracePeriodMillis = 100, timeoutMillis = 1000)
    }
}

