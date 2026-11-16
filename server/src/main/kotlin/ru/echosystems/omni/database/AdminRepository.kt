package ru.echosystems.omni.database

import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

@Serializable
data class ReferenceUpsertRequest(
    val name: String,
    val description: String? = null,
    val building: String? = null,
    val roomNumber: String? = null,
    val categoryId: Long? = null,
    val manufacturerId: Long? = null
)

@Serializable
data class AdminReferenceItem(
    val id: Long,
    val name: String,
    val description: String? = null,
    val building: String? = null,
    val roomNumber: String? = null,
    val categoryId: Long? = null,
    val manufacturerId: Long? = null
)

enum class AdminReferenceType(val slug: String, val table: String, vararg val columns: String) {
    CATEGORIES("categories", "equipment_categories", "description"),
    MANUFACTURERS("manufacturers", "manufacturers"),
    LOCATIONS("locations", "locations", "description", "building", "room_number"),
    DEPARTMENTS("departments", "departments", "description"),
    MODELS("models", "equipment_models", "description", "category_id", "manufacturer_id");

    fun accepts(request: ReferenceUpsertRequest): Boolean =
        request.name.isNotBlank() && request.name.length <= (if (this == MODELS) 150 else 100) &&
            when (this) {
                MODELS -> (request.categoryId ?: 0) > 0 && (request.manufacturerId ?: 0) > 0
                LOCATIONS -> (request.building?.length ?: 0) <= 100 && (request.roomNumber?.length ?: 0) <= 30
                else -> true
            }

    fun values(request: ReferenceUpsertRequest): List<Any?> = listOf(request.name.trim()) + columns.map { column ->
        when (column) {
            "description" -> request.description
            "building" -> request.building
            "room_number" -> request.roomNumber
            "category_id" -> request.categoryId
            "manufacturer_id" -> request.manufacturerId
            else -> error("Unsupported reference column")
        }
    }

    companion object {
        fun fromSlug(slug: String?) = entries.firstOrNull { it.slug == slug }
    }
}

@Serializable
data class UserCreateRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val email: String? = null,
    val role: String = "operator"
)

class AdminRepository {
    suspend fun references(type: AdminReferenceType): List<AdminReferenceItem> = dbQuery { connection ->
        val columns = (listOf("id", "name") + type.columns).joinToString()
        connection.prepareStatement("SELECT $columns FROM inventory.${type.table} ORDER BY name, id").use { statement ->
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        AdminReferenceItem(
                            id = rows.getLong("id"),
                            name = rows.getString("name"),
                            description = if ("description" in type.columns) rows.getString("description") else null,
                            building = if (type == AdminReferenceType.LOCATIONS) rows.getString("building") else null,
                            roomNumber = if (type == AdminReferenceType.LOCATIONS) rows.getString("room_number") else null,
                            categoryId = if (type == AdminReferenceType.MODELS) rows.getLong("category_id") else null,
                            manufacturerId = if (type == AdminReferenceType.MODELS) rows.getLong("manufacturer_id") else null
                        )
                    )
                }
            }
        }
    }

    suspend fun createReference(type: AdminReferenceType, request: ReferenceUpsertRequest): Long = dbQuery { connection ->
        val columns = listOf("name") + type.columns
        val placeholders = columns.joinToString { column ->
            if (column == "building" || column == "room_number") "NULLIF(?, '')" else "?"
        }
        val sql = "INSERT INTO inventory.${type.table}(${columns.joinToString()}) VALUES ($placeholders) RETURNING id"
        connection.prepareStatement(sql).use { statement ->
            type.values(request).forEachIndexed { index, value -> statement.setObject(index + 1, value) }
            statement.executeQuery().use { rows -> rows.next(); rows.getLong(1) }
        }
    }

    suspend fun updateReference(type: AdminReferenceType, id: Long, request: ReferenceUpsertRequest): Boolean =
        dbQuery { connection ->
            // Older clients only sent name and description for locations.
            val columns = listOf("name") + type.columns
            val assignments = columns.joinToString { column ->
                if (column == "building" || column == "room_number") "$column=NULLIF(COALESCE(?, $column), '')" else "$column=?"
            }
            connection.prepareStatement("UPDATE inventory.${type.table} SET $assignments WHERE id=?").use { statement ->
                type.values(request).forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                statement.setLong(columns.size + 1, id)
                statement.executeUpdate() == 1
            }
        }

    suspend fun createUser(request: UserCreateRequest): UUID = dbQuery { c ->
        require(request.role in setOf("admin", "operator", "viewer")) { "Invalid role" }
        c.prepareStatement(
            """
            INSERT INTO inventory.app_users(username,password_hash,full_name,email,role)
            VALUES(?,?,?,?,?) RETURNING id
            """.trimIndent()
        ).use { s ->
            s.setString(1, request.username)
            s.setString(2, BCrypt.hashpw(request.password, BCrypt.gensalt(12)))
            s.setString(3, request.fullName)
            s.setString(4, request.email)
            s.setString(5, request.role)
            s.executeQuery().use { r -> r.next(); r.getObject(1, UUID::class.java) }
        }
    }

    suspend fun blockUser(id: UUID) = dbQuery { c ->
        c.prepareStatement("UPDATE inventory.app_users SET is_active=FALSE WHERE id=?").use { s ->
            s.setObject(1, id)
            check(s.executeUpdate() == 1) { "User not found" }
        }
    }

}
