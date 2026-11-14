package ru.echosystems.omni.database

import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

@Serializable
data class ReferenceUpsertRequest(val name: String, val description: String? = null)

@Serializable
data class UserCreateRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val email: String? = null,
    val role: String = "operator"
)

class AdminRepository {
    private val tables = setOf("categories", "manufacturers", "locations", "departments")

    suspend fun createReference(type: String, request: ReferenceUpsertRequest): Long = dbQuery { c ->
        val table = table(type)
        c.prepareStatement(
            "INSERT INTO inventory.$table(name, description) VALUES (?, ?) RETURNING id"
        ).use { s ->
            s.setString(1, request.name)
            s.setString(2, request.description)
            s.executeQuery().use { r -> r.next(); r.getLong(1) }
        }
    }

    suspend fun updateReference(type: String, id: Long, request: ReferenceUpsertRequest) = dbQuery { c ->
        val table = table(type)
        c.prepareStatement("UPDATE inventory.$table SET name=?, description=? WHERE id=?").use { s ->
            s.setString(1, request.name)
            s.setString(2, request.description)
            s.setLong(3, id)
            check(s.executeUpdate() == 1) { "Reference not found" }
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

    private fun table(type: String): String = when (type) {
        "categories" -> "equipment_categories"
        "manufacturers" -> "manufacturers"
        "locations" -> "locations"
        "departments" -> "departments"
        else -> error("Unsupported reference")
    }
}
