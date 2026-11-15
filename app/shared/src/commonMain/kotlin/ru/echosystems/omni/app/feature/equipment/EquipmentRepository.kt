package ru.echosystems.omni.app.feature.equipment

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.ApiException

class EquipmentRepository(private val apiClient: ApiClient) {
    suspend fun loadPage(search: String = "", offset: Long = 0, categoryId: Long? = null, locationId: Long? = null, status: String? = null): EquipmentPage {
        val response = apiClient.httpClient.get("/api/equipment") {
            parameter("search", search.takeIf { it.isNotBlank() })
            parameter("status", status)
            parameter("categoryId", categoryId)
            parameter("locationId", locationId)
            parameter("limit", 50)
            parameter("offset", offset)
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось загрузить список техники", response.bodyAsText())
        }
        return response.body()
    }

    suspend fun references(type: String): List<ReferenceItem> {
        require(type in setOf("categories", "locations", "models", "statuses", "departments"))
        val response = apiClient.httpClient.get("/api/references/$type")
        if (response.status.value !in 200..299) throw ApiException(
            response.status.value, "Не удалось загрузить справочник", ""
        )
        return response.body()
    }

    suspend fun loadDetails(id: Long): EquipmentDetails {
        val response = apiClient.httpClient.get("/api/equipment/$id")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось загрузить оборудование", response.bodyAsText())
        }
        return response.body()
    }

    suspend fun loadHistory(id: Long): List<EquipmentHistoryItem> {
        val response = apiClient.httpClient.get("/api/equipment/$id/history")
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось загрузить историю", response.bodyAsText())
        }
        return response.body()
    }

    suspend fun changeStatus(id: Long, statusCode: String, comment: String? = null) {
        val response = apiClient.httpClient.post("/api/equipment/$id/status") {
            contentType(ContentType.Application.Json)
            setBody(ChangeEquipmentStatusRequest(statusCode, comment))
        }
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, "Не удалось изменить статус", response.bodyAsText())
        }
    }

    suspend fun save(id: Long?, request: EquipmentFormRequest): Long {
        val response = if (id == null) {
            apiClient.httpClient.post("/api/equipment") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } else {
            apiClient.httpClient.put("/api/equipment/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        if (response.status.value !in 200..299) {
            val message = when (response.status.value) {
                400 -> "Проверьте даты, стоимость и выбранные значения"
                401 -> "Сессия истекла. Войдите снова."
                403 -> "Для сохранения нужны права администратора"
                404 -> "Запись оборудования не найдена"
                409 -> "Инвентарный или серийный номер уже используется"
                else -> "Не удалось сохранить оборудование"
            }
            throw ApiException(response.status.value, message, "")
        }
        return id ?: response.body<CreatedEquipment>().id
    }
}
