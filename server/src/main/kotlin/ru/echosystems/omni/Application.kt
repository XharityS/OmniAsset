package ru.echosystems.omni

import java.sql.SQLException
import io.ktor.http.HttpStatusCode
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
import ru.echosystems.omni.database.AdminRepository
import ru.echosystems.omni.database.ReferenceUpsertRequest
import ru.echosystems.omni.database.UserCreateRequest
import ru.echosystems.omni.database.DirectoryAdminRepository
import ru.echosystems.omni.database.EmployeeUpsertRequest
import ru.echosystems.omni.database.EquipmentCrudRepository
import ru.echosystems.omni.database.EquipmentUpsertRequest
import ru.echosystems.omni.database.DatabaseFactory
import ru.echosystems.omni.database.EquipmentActionsRepository
import ru.echosystems.omni.database.EquipmentRepository
import ru.echosystems.omni.database.MoveEquipmentRequest
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

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "omniasset"
            verifier(jwtService.verifier)
            validate { credential ->
                if (credential.payload.subject != null &&
                    credential.payload.getClaim("role").asString() != null
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    DatabaseFactory.init(environment.config)
    val authRepository = AuthRepository()
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
                actionsRepository.moveEquipment(equipmentId, call.receive<MoveEquipmentRequest>())
                call.respondText("Equipment moved")
            }

            post("/api/equipment/{id}/status") {
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
                val page = equipmentRepository.findPage(
                    search = call.request.queryParameters["search"],
                    statusCode = call.request.queryParameters["status"],
                    categoryId = call.request.queryParameters["categoryId"]?.toLongOrNull(),
                    locationId = call.request.queryParameters["locationId"]?.toLongOrNull(),
                    employeeId = call.request.queryParameters["employeeId"]?.toLongOrNull(),
                    limit = 100,
                    offset = 0
                )
                val csv = buildString {
                    appendLine("id;inventory_number;serial_number;model;manufacturer;category;status;location;department;responsible")
                    page.items.forEach { item ->
                        appendLine(listOf(item.id,item.inventoryNumber,item.serialNumber,item.modelName,
                            item.manufacturerName,item.categoryName,item.statusName,item.locationName,
                            item.departmentName,item.responsibleName).joinToString(";") { value ->
                            value?.toString()?.replace(";", ",") ?: ""
                        })
                    }
                }
                call.respondText(csv, io.ktor.http.ContentType.Text.CSV)
            }

            post("/api/admin/references/{type}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val type = call.parameters["type"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val id = adminRepository.createReference(type, call.receive<ReferenceUpsertRequest>())
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/api/admin/references/{type}/{id}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@put call.respond(HttpStatusCode.Forbidden)
                val type = call.parameters["type"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                adminRepository.updateReference(type, id, call.receive<ReferenceUpsertRequest>())
                call.respondText("Reference updated")
            }

            post("/api/admin/users") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val id = adminRepository.createUser(call.receive<UserCreateRequest>())
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }

            post("/api/admin/users/{id}/block") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@post call.respond(HttpStatusCode.Forbidden)
                val id = runCatching { java.util.UUID.fromString(call.parameters["id"]) }
                    .getOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                adminRepository.blockUser(id)
                call.respondText("User blocked")
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
                val id = directoryAdminRepository.createEmployee(call.receive<EmployeeUpsertRequest>())
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            put("/api/admin/employees/{id}") {
                if (call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != "admin")
                    return@put call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                directoryAdminRepository.updateEmployee(id, call.receive<EmployeeUpsertRequest>())
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
