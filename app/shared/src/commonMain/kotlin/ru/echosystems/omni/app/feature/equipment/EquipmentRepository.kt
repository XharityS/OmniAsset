package ru.echosystems.omni.app.feature.equipment

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
}
