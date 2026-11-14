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
            jdbcUrl = config.propertyOrNull("database.jdbcUrl")?.getString()
                ?: "jdbc:postgresql://localhost:5432/omniasset?currentSchema=inventory"
            username = config.propertyOrNull("database.username")?.getString() ?: "postgres"
            password = config.propertyOrNull("database.password")?.getString() ?: ""
            maximumPoolSize = config.propertyOrNull("database.maximumPoolSize")
                ?.getString()?.toIntOrNull() ?: 15
            minimumIdle = config.propertyOrNull("database.minimumIdle")
                ?.getString()?.toIntOrNull() ?: 5
            connectionTimeout = config.propertyOrNull("database.connectionTimeout")
                ?.getString()?.toLongOrNull() ?: 5_000
            validationTimeout = config.propertyOrNull("database.validationTimeout")
                ?.getString()?.toLongOrNull() ?: 3_000
            idleTimeout = config.propertyOrNull("database.idleTimeout")
                ?.getString()?.toLongOrNull() ?: 600_000
            maxLifetime = config.propertyOrNull("database.maxLifetime")
                ?.getString()?.toLongOrNull() ?: 1_800_000
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

    fun close() {
        if (::dataSource.isInitialized) dataSource.close()
    }
}
