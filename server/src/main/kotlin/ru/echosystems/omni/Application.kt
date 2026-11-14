package ru.echosystems.omni

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import ru.echosystems.omni.database.DatabaseFactory
import ru.echosystems.omni.database.EquipmentRepository

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)
    val equipmentRepository = EquipmentRepository()
    val reportSemaphore = Semaphore(permits = 3)

    environment.monitor.subscribe(ApplicationStopping) {
        DatabaseFactory.close()
    }

    routing {
        get("/") {
            call.respondText("OmniAsset server is running")
        }

        get("/health") {
            call.respondText("ok")
        }

        get("/api/equipment") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0

            val equipment = equipmentRepository.findPage(limit, offset)
            call.respond(equipment)
        }

        get("/api/reports/equipment") {
            reportSemaphore.withPermit {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                call.respond(equipmentRepository.findPage(limit, offset))
            }
        }
    }
}
