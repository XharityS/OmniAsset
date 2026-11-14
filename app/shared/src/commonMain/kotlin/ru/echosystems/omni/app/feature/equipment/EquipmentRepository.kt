package ru.echosystems.omni.app.feature.equipment

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.ApiException

class EquipmentRepository(private val apiClient: ApiClient) {
    suspend fun loadPage(search: String = ""): EquipmentPage {
        val response = apiClient.httpClient.get("/api/equipment") {
            parameter("search", search.takeIf { it.isNotBlank() })
            parameter("limit", 50)
            parameter("offset", 0)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось загрузить список техники", response.body())
        }
        return response.body()
    }

    suspend fun loadDetails(id: Long): EquipmentDetails {
        val response = apiClient.httpClient.get("/api/equipment/$id")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось загрузить оборудование", response.body())
        }
        return response.body()
    }

    suspend fun loadHistory(id: Long): List<EquipmentHistoryItem> {
        val response = apiClient.httpClient.get("/api/equipment/$id/history")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось загрузить историю", response.body())
        }
        return response.body()
    }

    suspend fun changeStatus(id: Long, statusCode: String, comment: String? = null) {
        val response = apiClient.httpClient.post("/api/equipment/$id/status") {
            contentType(ContentType.Application.Json)
            setBody(ChangeEquipmentStatusRequest(statusCode, comment))
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось изменить статус", response.body())
        }
    }
}
