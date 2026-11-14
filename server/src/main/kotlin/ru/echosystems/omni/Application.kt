package ru.echosystems.omni

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
import ru.echosystems.omni.database.DatabaseFactory
import ru.echosystems.omni.database.EquipmentActionsRepository
import ru.echosystems.omni.database.EquipmentRepository
import ru.echosystems.omni.database.MoveEquipmentRequest

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
