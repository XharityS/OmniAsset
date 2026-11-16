package ru.echosystems.omni

import java.sql.SQLException
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import ru.echosystems.omni.auth.AuthRepository
import ru.echosystems.omni.auth.JwtService
import ru.echosystems.omni.auth.LoginRequest
import ru.echosystems.omni.database.ChangeEquipmentStatusRequest
import ru.echosystems.omni.database.AdminReferenceType
import ru.echosystems.omni.database.AdminRepository
import ru.echosystems.omni.database.ReferenceUpsertRequest
import ru.echosystems.omni.database.UserCreateRequest
import ru.echosystems.omni.database.UserUpdateRequest
import ru.echosystems.omni.database.UserChangeResult
import java.util.UUID
import ru.echosystems.omni.database.DirectoryAdminRepository
import ru.echosystems.omni.database.EmployeeUpsertRequest
import ru.echosystems.omni.database.EquipmentCrudRepository
import ru.echosystems.omni.database.EquipmentUpsertRequest
import ru.echosystems.omni.database.DatabaseFactory
import ru.echosystems.omni.database.EquipmentActionsRepository
import ru.echosystems.omni.database.EquipmentRepository
import ru.echosystems.omni.database.MoveEquipmentRequest
import ru.echosystems.omni.database.MoveResult
import ru.echosystems.omni.database.ReferenceRepository

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    val jwtSecret = System.getenv("JWT_SECRET")
        ?.takeIf { it.isNotBlank() }
        ?: environment.config.propertyOrNull("jwt.secret")
            ?.getString()
            ?.takeIf { it.isNotBlank() }
        ?: error("JWT_SECRET must be configured")

    val jwtService = JwtService(
        secret = jwtSecret,
        issuer = environment.config.propertyOrNull("jwt.issuer")?.getString() ?: "omniasset",
        audience = environment.config.propertyOrNull("jwt.audience")?.getString()
            ?: "omniasset-clients",
        validityMs = environment.config.propertyOrNull("jwt.validityMs")
            ?.getString()?.toLongOrNull() ?: 3_600_000
    )

    val authRepository = AuthRepository()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "omniasset"
            verifier(jwtService.verifier)
            validate { credential ->
                val id = credential.payload.subject.toUserId()
                val role = credential.payload.getClaim("role").asString()
                if (id != null && role != null && authRepository.hasActiveRole(id, role)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    DatabaseFactory.init(environment.config)
    val equipmentRepository = EquipmentRepository()
    val equipmentCrudRepository = EquipmentCrudRepository()
    val referenceRepository = ReferenceRepository()
    val adminRepository = AdminRepository()
    val directoryAdminRepository = DirectoryAdminRepository()
    val actionsRepository = EquipmentActionsRepository()
    val reportSemaphore = Semaphore(permits = 3)

    monitor.subscribe(ApplicationStopping) {
        DatabaseFactory.close()
    }

    routing {
        get("/") {
            call.respondText("OmniAsset server is running")
        }

        get("/health") {
            call.respondText("ok")
        }

        post("/api/auth/login") {
            val request = call.receive<LoginRequest>()
            val user = authRepository.authenticate(request.username, request.password)
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    "Invalid username or password"
                )

            call.respond(
                ru.echosystems.omni.auth.LoginResponse(
                    token = jwtService.createToken(user),
                    userId = user.id.toString(),
                    username = user.username,
                    fullName = user.fullName,
                    role = user.role
                )
            )
        }

        authenticate("auth-jwt") {
            get("/api/equipment") {
                call.respond(
                    equipmentRepository.findPage(
                        search = call.request.queryParameters["search"],
                        statusCode = call.request.queryParameters["status"],
                        categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull(),
                        locationId = call.request.queryParameters["locationId"]?.toLongOrNull(),
                        employeeId = call.request.queryParameters["employeeId"]?.toLongOrNull(),
                        limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                        offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                    )
                )
            }

            post("/api/equipment/{id}/move") {
                val equipmentId = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respondText(
                        "Invalid equipment id",
                        status = HttpStatusCode.BadRequest
                    )
                val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                if (role != "admin" && role != "operator") return@post call.respond(HttpStatusCode.Forbidden)
                val request = call.receive<MoveEquipmentRequest>()
                val result = call.saveEquipment { actionsRepository.moveEquipment(equipmentId, request) }
                    ?: return@post
                when (result) {
                    MoveResult.SAVED -> call.respondText("Equipment moved")
                    MoveResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound)
                    MoveResult.NOT_MOVABLE -> call.respond(HttpStatusCode.Conflict)
                    MoveResult.EMPLOYEE_UNAVAILABLE -> call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/api/equipment/{id}/status") {
                val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                if (role != "admin" && role != "operator") return@post call.respond(HttpStatusCode.Forbidden)
                val equipmentId = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respondText(
                        "Invalid equipment id",
                        status = HttpStatusCode.BadRequest
                    )
                actionsRepository.changeStatus(
                    equipmentId,
                    call.receive<ChangeEquipmentStatusRequest>()
                )
                call.respondText("Equipment status changed")
            }

            get("/api/equipment/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respondText("Invalid equipment id", status = HttpStatusCode.BadRequest)
                val item = equipmentCrudRepository.findById(id)
                    ?: return@get call.respondText("Equipment not found", status = HttpStatusCode.NotFound)
                call.respond(item)
            }

            get("/api/equipment/{id}/history") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respondText("Invalid equipment id", status = HttpStatusCode.BadRequest)
                call.respond(equipmentCrudRepository.history(id))
            }

            post("/api/equipment") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val id = call.saveEquipment { equipmentCrudRepository.create(call.receive<EquipmentUpsertRequest>()) }
                    ?: return@post
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/api/equipment/{id}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@put call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respondText("Invalid equipment id", status = HttpStatusCode.BadRequest)
                val updated = call.saveEquipment { equipmentCrudRepository.update(id, call.receive<EquipmentUpsertRequest>()) }
                    ?: return@put
                if (!updated) return@put call.respond(HttpStatusCode.NotFound)
                call.respondText("Equipment updated")
            }

            post("/api/equipment/{id}/archive") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                actionsRepository.changeStatus(
                    id,
                    ChangeEquipmentStatusRequest("written_off", "Equipment archived")
                )
                call.respondText("Equipment archived")
            }

            get("/api/summary/statuses") {
                call.respond(equipmentCrudRepository.summary())
            }

            get("/api/equipment.csv") {
                val query = call.request.queryParameters
                val search = query["search"]
                val status = query["status"]
                val categoryId = query["categoryId"]?.toLongOrNull()
                val locationId = query["locationId"]?.toLongOrNull()
                val employeeId = query["employeeId"]?.toLongOrNull()

                suspend fun page(afterId: Long?) = equipmentRepository.findPage(
                    search = search,
                    statusCode = status,
                    categoryId = categoryId,
                    locationId = locationId,
                    employeeId = employeeId,
                    limit = 100,
                    offset = 0,
                    afterId = afterId
                )

                val firstPage = reportSemaphore.withPermit { page(null) }
                call.response.header("Content-Disposition", "attachment; filename=omniasset-equipment.csv")
                call.respondTextWriter(io.ktor.http.ContentType.Text.CSV.withCharset(Charsets.UTF_8)) {
                    write("\uFEFF")
                    write("id;inventory_number;serial_number;model;manufacturer;category;status;location;department;responsible\r\n")
                    var current = firstPage
                    while (true) {
                        for (item in current.items) {
                            write(listOf(
                                item.id, item.inventoryNumber, item.serialNumber, item.modelName,
                                item.manufacturerName, item.categoryName, item.statusName, item.locationName,
                                item.departmentName, item.responsibleName
                            ).joinToString(";") { csvCell(it) })
                            write("\r\n")
                        }
                        if (!current.hasMore) break
                        val afterId = current.items.last().id
                        current = reportSemaphore.withPermit { page(afterId) }
                    }
                }
            }

            get("/api/admin/references/{type}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@get call.respond(HttpStatusCode.Forbidden)
                val type = AdminReferenceType.fromSlug(call.parameters["type"])
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(adminRepository.references(type))
            }

            post("/api/admin/references/{type}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val type = AdminReferenceType.fromSlug(call.parameters["type"])
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<ReferenceUpsertRequest>()
                if (!type.accepts(request)) return@post call.respond(HttpStatusCode.BadRequest)
                val id = call.saveEquipment { adminRepository.createReference(type, request) } ?: return@post
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/api/admin/references/{type}/{id}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@put call.respond(HttpStatusCode.Forbidden)
                val type = AdminReferenceType.fromSlug(call.parameters["type"])
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<ReferenceUpsertRequest>()
                if (!type.accepts(request)) return@put call.respond(HttpStatusCode.BadRequest)
                val updated = call.saveEquipment { adminRepository.updateReference(type, id, request) } ?: return@put
                if (!updated) return@put call.respond(HttpStatusCode.NotFound)
                call.respondText("Reference updated")
            }

            post("/api/admin/users") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val request = call.receive<UserCreateRequest>()
                if (request.username.isBlank() || request.username.length > 50 ||
                    request.password.length < 8 || request.password.encodeToByteArray().size > 72 ||
                    !validUserProfile(request.fullName, request.email, request.role)
                ) return@post call.respond(HttpStatusCode.BadRequest)
                val id = call.saveEquipment { adminRepository.createUser(request.copy(
                    username = request.username.trim(), fullName = request.fullName.trim(),
                    email = request.email?.trim()?.takeIf { it.isNotEmpty() }
                )) } ?: return@post
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }

            put("/api/admin/users/{id}") {
                val actor = call.adminUserId() ?: return@put call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"].toUserId() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UserUpdateRequest>()
                if (!validUserProfile(request.fullName, request.email, request.role))
                    return@put call.respond(HttpStatusCode.BadRequest)
                val result = call.saveEquipment { adminRepository.updateUser(actor, id, request) } ?: return@put
                call.respondUserChange(result)
            }

            post("/api/admin/users/{id}/block") {
                val actor = call.adminUserId() ?: return@post call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"].toUserId() ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respondUserChange(adminRepository.blockUser(actor, id))
            }

            get("/api/admin/users") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@get call.respond(HttpStatusCode.Forbidden)
                call.respond(directoryAdminRepository.users())
            }

            get("/api/admin/employees") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@get call.respond(HttpStatusCode.Forbidden)
                call.respond(directoryAdminRepository.employees())
            }

            post("/api/admin/employees") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val request = call.receive<EmployeeUpsertRequest>()
                if (request.fullName.isBlank()) return@post call.respond(HttpStatusCode.BadRequest)
                val id = call.saveEquipment { directoryAdminRepository.createEmployee(request) } ?: return@post
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/api/admin/employees/{id}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@put call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<EmployeeUpsertRequest>()
                if (request.fullName.isBlank()) return@put call.respond(HttpStatusCode.BadRequest)
                val updated = call.saveEquipment { directoryAdminRepository.updateEmployee(id, request) } ?: return@put
                if (!updated) return@put call.respond(HttpStatusCode.NotFound)
                call.respondText("Employee updated")
            }

            post("/api/admin/employees/{id}/block") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                directoryAdminRepository.blockEmployee(id)
                call.respondText("Employee blocked")
            }

            get("/api/references/{type}") {
                val type = call.parameters["type"]
                    ?: return@get call.respondText("Reference type is required", status = HttpStatusCode.BadRequest)
                call.respond(referenceRepository.list(type))
            }

            get("/api/reports/equipment") {
                reportSemaphore.withPermit {
                    call.respond(
                        equipmentRepository.findPage(
                            search = call.request.queryParameters["search"],
                            statusCode = call.request.queryParameters["status"],
                            categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull(),
                            locationId = call.request.queryParameters["locationId"]?.toLongOrNull(),
                            employeeId = call.request.queryParameters["employeeId"]?.toLongOrNull(),
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                            offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                        )
                    )
                }
            }
        }
    }
}

private suspend fun <T : Any> ApplicationCall.saveEquipment(save: suspend () -> T): T? =
    try {
        save()
    } catch (error: SQLException) {
        when (error.sqlState) {
            "23505" -> respond(HttpStatusCode.Conflict)
            "23503", "23514", "22001", "22003", "22007", "22008", "22P02" ->
                respond(HttpStatusCode.BadRequest)
            else -> throw error
        }
        null
    }

private fun csvCell(value: Any?): String {
    val text = value?.toString().orEmpty()
    val first = text.trimStart().firstOrNull()
    val safeText = if (first != null && first in "=+-@") "'" + text else text
    return if (safeText.any { it == ';' || it == '"' || it == '\r' || it == '\n' }) {
        "\"" + safeText.replace("\"", "\"\"") + "\""
    } else safeText
}

private fun String?.toUserId(): UUID? = this?.let { runCatching { UUID.fromString(it) }.getOrNull() }

private fun ApplicationCall.adminUserId(): UUID? = principal<JWTPrincipal>()?.payload
    ?.takeIf { it.getClaim("role").asString() == "admin" }?.subject.toUserId()

private fun validUserProfile(fullName: String, email: String?, role: String): Boolean =
    fullName.isNotBlank() && fullName.length <= 150 && (email?.length ?: 0) <= 150 &&
        role in setOf("admin", "operator", "viewer")

private suspend fun ApplicationCall.respondUserChange(result: UserChangeResult) {
    respond(when (result) {
        UserChangeResult.UPDATED -> HttpStatusCode.NoContent
        UserChangeResult.FORBIDDEN -> HttpStatusCode.Forbidden
        UserChangeResult.NOT_FOUND -> HttpStatusCode.NotFound
    })
}
