package ru.echosystems.omni.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    lateinit var dataSource: HikariDataSource
        private set

    fun init(config: ApplicationConfig): DataSource {
        val hikari = HikariConfig().apply {
            jdbcUrl = envOrConfig(
                envName = "DATABASE_JDBC_URL",
                config = config,
                configPath = "database.jdbcUrl"
            ) ?: "jdbc:postgresql://localhost:5432/omniasset?currentSchema=inventory"
            username = envOrConfig("DATABASE_USERNAME", config, "database.username") ?: "postgres"
            password = envOrConfig("DATABASE_PASSWORD", config, "database.password") ?: ""
            maximumPoolSize = envOrConfig("DATABASE_MAX_POOL_SIZE", config, "database.maximumPoolSize")
                ?.toIntOrNull() ?: 15
            minimumIdle = envOrConfig("DATABASE_MIN_IDLE", config, "database.minimumIdle")
                ?.toIntOrNull() ?: 5
            connectionTimeout = envOrConfig("DATABASE_CONNECTION_TIMEOUT", config, "database.connectionTimeout")
                ?.toLongOrNull() ?: 5_000
            validationTimeout = envOrConfig("DATABASE_VALIDATION_TIMEOUT", config, "database.validationTimeout")
                ?.toLongOrNull() ?: 3_000
            idleTimeout = envOrConfig("DATABASE_IDLE_TIMEOUT", config, "database.idleTimeout")
                ?.toLongOrNull() ?: 600_000
            maxLifetime = envOrConfig("DATABASE_MAX_LIFETIME", config, "database.maxLifetime")
                ?.toLongOrNull() ?: 1_800_000
            poolName = "omniasset-postgres"
            leakDetectionThreshold = 10_000
        }

        dataSource = HikariDataSource(hikari)

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("SELECT 1")
            }
        }

        Flyway.configure()
            .dataSource(dataSource)
            .schemas("inventory")
            .defaultSchema("inventory")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load()
            .migrate()

        logger.info(
            "PostgreSQL connected: pool={}, maxSize={}",
            dataSource.poolName,
            dataSource.maximumPoolSize
        )
        return dataSource
    }

    private fun envOrConfig(
        envName: String,
        config: ApplicationConfig,
        configPath: String
    ): String? =
        System.getenv(envName)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull(configPath)?.getString()?.takeIf { it.isNotBlank() }

    fun close() {
        if (::dataSource.isInitialized) dataSource.close()
    }
}
