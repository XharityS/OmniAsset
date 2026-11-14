package ru.echosystems.omni.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

suspend fun <T> dbQuery(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        DatabaseFactory.dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            }
        }
    }
