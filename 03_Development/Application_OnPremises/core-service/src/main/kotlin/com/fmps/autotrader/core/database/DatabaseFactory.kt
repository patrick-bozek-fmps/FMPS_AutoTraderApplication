package com.fmps.autotrader.core.database

import com.fmps.autotrader.core.database.schema.*
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import javax.sql.DataSource

/**
 * Database Factory for initializing and managing database connections
 * 
 * Responsibilities:
 * - Configure HikariCP connection pool
 * - Run Flyway migrations
 * - Initialize Exposed ORM
 * - Provide transaction support
 * - Manage database lifecycle
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private lateinit var dataSource: HikariDataSource
    private lateinit var database: Database
    
    /**
     * Initialize the database with configuration
     * 
     * @param config Application configuration
     */
    fun init(config: Config) {
        logger.info("Initializing database...")
        
        // Ensure data directory exists
        val dbUrl = config.getString("database.url")
        val dbPath = dbUrl.removePrefix("jdbc:sqlite:")
        val dbFile = File(dbPath)
        dbFile.parentFile?.mkdirs()
        
        logger.info("Database path: ${dbFile.absolutePath}")
        
        // Configure HikariCP connection pool
        dataSource = createDataSource(config)
        
        // Run Flyway migrations
        runMigrations(config)
        
        // Initialize Exposed
        database = Database.connect(dataSource)
        
        // Verify schema (in development mode)
        if (config.hasPath("app.environment") && 
            config.getString("app.environment") == "development") {
            verifySchema()
        }
        
        logger.info("Database initialized successfully")
    }
    
    /**
     * Create HikariCP data source
     */
    private fun createDataSource(config: Config): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            driverClassName = config.getString("database.driver")
            jdbcUrl = config.getString("database.url")
            
            // HikariCP settings
            maximumPoolSize = config.getInt("database.hikari.maximumPoolSize")
            minimumIdle = config.getInt("database.hikari.minimumIdle")
            idleTimeout = config.getLong("database.hikari.idleTimeout")
            connectionTimeout = config.getLong("database.hikari.connectionTimeout")
            maxLifetime = config.getLong("database.hikari.maxLifetime")
            isAutoCommit = config.getBoolean("database.hikari.autoCommit")
            poolName = config.getString("database.hikari.poolName")
            
            // SQLite optimizations
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            
            // Health check
            connectionTestQuery = "SELECT 1"
            
            // Logging
            validate()
        }
        
        logger.info("HikariCP connection pool configured: ${hikariConfig.poolName}")
        return HikariDataSource(hikariConfig)
    }
    
    /**
     * Run Flyway database migrations
     */
    private fun runMigrations(config: Config) {
        logger.info("Running database migrations...")
        
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*config.getStringList("database.flyway.locations").toTypedArray())
            .baselineOnMigrate(config.getBoolean("database.flyway.baselineOnMigrate"))
            .baselineVersion(config.getString("database.flyway.baselineVersion"))
            .validateOnMigrate(config.getBoolean("database.flyway.validateOnMigrate"))
            .cleanDisabled(config.getBoolean("database.flyway.cleanDisabled"))
            .load()
        
        try {
            val result = flyway.migrate()
            logger.info("Flyway migrations completed: ${result.migrationsExecuted} migrations applied")
            
            // Log migration details
            if (result.success) {
                logger.info("Flyway migration successful")
            } else {
                logger.warn("Flyway migration completed with warnings")
            }
        } catch (e: Exception) {
            logger.error("Flyway migration failed", e)
            throw e
        }
    }
    
    /**
     * Verify database schema (development only)
     */
    private fun verifySchema() {
        logger.debug("Verifying database schema...")
        
        transaction(database) {
            // Verify all tables exist
            val tables = listOf(
                AITradersTable,
                TradesTable,
                PatternsTable,
                ConfigurationsTable,
                ExchangeAccountsTable
            )
            
            tables.forEach { table ->
                try {
                    // This will fail if table doesn't exist
                    SchemaUtils.checkCycle(table)
                    logger.debug("✓ Table ${table.tableName} verified")
                } catch (e: Exception) {
                    logger.error("✗ Table ${table.tableName} verification failed", e)
                }
            }
        }
        
        logger.debug("Schema verification complete")
    }
    
    /**
     * Execute a database query within a coroutine context
     * 
     * @param block The database operation to execute
     * @return Result of the operation
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) {
            block()
        }
    
    /**
     * Get the current database instance
     * 
     * @return The Exposed Database instance
     */
    fun getDatabase(): Database {
        if (!::database.isInitialized) {
            throw IllegalStateException("Database not initialized. Call init() first.")
        }
        return database
    }
    
    /**
     * Get the current data source
     * 
     * @return The HikariCP DataSource
     */
    fun getDataSource(): DataSource {
        if (!::dataSource.isInitialized) {
            throw IllegalStateException("DataSource not initialized. Call init() first.")
        }
        return dataSource
    }
    
    /**
     * Close the database connection pool
     * 
     * Call this on application shutdown
     */
    fun close() {
        logger.info("Closing database connections...")
        
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            dataSource.close()
            logger.info("Database connections closed")
        }
    }
    
    /**
     * Get database statistics
     * 
     * @return Map of database statistics
     */
    fun getStatistics(): Map<String, Any> {
        if (!::dataSource.isInitialized) {
            return emptyMap()
        }
        
        val poolStats = dataSource.hikariPoolMXBean
        
        return mapOf(
            "poolName" to dataSource.poolName,
            "activeConnections" to poolStats.activeConnections,
            "idleConnections" to poolStats.idleConnections,
            "totalConnections" to poolStats.totalConnections,
            "threadsAwaitingConnection" to poolStats.threadsAwaitingConnection,
            "maxPoolSize" to dataSource.maximumPoolSize,
            "minIdle" to dataSource.minimumIdle
        )
    }
}

