package com.fmps.autotrader.core.database

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseFactoryTest {
    
    private val testDbPath = "./build/test-db/test-autotrader.db"
    
    @BeforeAll
    fun setup() {
        // Clean up any existing test database
        File(testDbPath).delete()
        
        // Create test configuration
        System.setProperty("database.url", "jdbc:sqlite:$testDbPath")
        System.setProperty("app.environment", "test")
        
        // Initialize database with test config
        val config = ConfigFactory.load()
        DatabaseFactory.init(config)
    }
    
    @AfterAll
    fun teardown() {
        DatabaseFactory.close()
        // Clean up test database
        File(testDbPath).delete()
    }
    
    @Test
    fun `should initialize database successfully`() {
        val database = DatabaseFactory.getDatabase()
        assertNotNull(database)
    }
    
    @Test
    fun `should return data source`() {
        val dataSource = DatabaseFactory.getDataSource()
        assertNotNull(dataSource)
        
        // Verify connection is working
        dataSource.connection.use { connection ->
            assertTrue(connection.isValid(5))
        }
    }
    
    @Test
    fun `should provide database statistics`() {
        val stats = DatabaseFactory.getStatistics()
        
        assertNotNull(stats)
        assertTrue(stats.containsKey("poolName"))
        assertTrue(stats.containsKey("activeConnections"))
        assertTrue(stats.containsKey("totalConnections"))
        assertTrue(stats.containsKey("maxPoolSize"))
        
        // Verify values are reasonable
        val maxPoolSize = stats["maxPoolSize"] as Int
        assertTrue(maxPoolSize > 0)
    }
    
    @Test
    fun `should execute database queries`() = runBlocking {
        val result = DatabaseFactory.dbQuery {
            // Simple test query
            "test"
        }
        
        assertEquals("test", result)
    }
}

