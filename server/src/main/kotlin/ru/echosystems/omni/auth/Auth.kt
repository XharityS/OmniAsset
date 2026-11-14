package ru.echosystems.omni.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import ru.echosystems.omni.database.dbQuery
import java.util.Date
import java.util.UUID

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
    val username: String,
    val fullName: String,
    val role: String
)

data class AuthUser(
    val id: UUID,
    val username: String,
    val fullName: String,
    val role: String
)

class AuthRepository {
    suspend fun authenticate(username: String, password: String): AuthUser? =
        dbQuery { connection ->
            connection.prepareStatement(
                """
                SELECT id, username, password_hash, full_name, role
                FROM inventory.app_users
                WHERE username = ? AND is_active = TRUE
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, username)
                statement.executeQuery().use { result ->
                    if (!result.next()) return@dbQuery null

                    val hash = result.getString("password_hash")
                    if (!BCrypt.checkpw(password, hash)) return@dbQuery null

                    AuthUser(
                        id = result.getObject("id", UUID::class.java),
                        username = result.getString("username"),
                        fullName = result.getString("full_name"),
                        role = result.getString("role")
                    )
                }
            }
        }
}

class JwtService(
    secret: String,
    private val issuer: String,
    private val audience: String,
    private val validityMs: Long
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun createToken(user: AuthUser): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id.toString())
            .withClaim("username", user.username)
            .withClaim("role", user.role)
            .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
            .sign(algorithm)
}
