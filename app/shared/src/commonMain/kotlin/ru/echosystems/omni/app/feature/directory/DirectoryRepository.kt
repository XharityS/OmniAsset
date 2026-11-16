package ru.echosystems.omni.app.feature.directory

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.ApiException

internal enum class DirectoryType(val slug: String, val title: String) {
    CATEGORIES("categories", "Категории"),
    MANUFACTURERS("manufacturers", "Производители"),
    LOCATIONS("locations", "Кабинеты и места"),
    DEPARTMENTS("departments", "Подразделения"),
    MODELS("models", "Модели"),
    EMPLOYEES("employees", "Сотрудники");

    val path: String
        get() = if (this == EMPLOYEES) "/api/admin/employees" else "/api/admin/references/$slug"
}

@Serializable
internal data class DirectoryEntry(
    val id: Long? = null,
    val name: String = "",
    val description: String? = null,
    val building: String? = null,
    val roomNumber: String? = null,
    val categoryId: Long? = null,
    val manufacturerId: Long? = null,
    val position: String? = null,
    val department: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val isActive: Boolean = true
)

@Serializable
private data class EmployeeRecord(
    val id: Long,
    val fullName: String,
    val position: String? = null,
    val department: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val isActive: Boolean = true
)

@Serializable
private data class EmployeeRequest(
    val fullName: String,
    val position: String?,
    val department: String?,
    val phone: String?,
    val email: String?
)

@Serializable
private data class ReferenceRequest(
    val name: String,
    val description: String?,
    val building: String?,
    val roomNumber: String?,
    val categoryId: Long?,
    val manufacturerId: Long?
)

internal class DirectoryRepository(apiClient: ApiClient) {
    private val client = apiClient.httpClient

    suspend fun list(type: DirectoryType): List<DirectoryEntry> {
        val response = client.get(type.path)
        response.requireSuccess("Не удалось загрузить справочник")
        return if (type == DirectoryType.EMPLOYEES) response.body<List<EmployeeRecord>>().map {
            DirectoryEntry(
                id = it.id, name = it.fullName, position = it.position, department = it.department,
                phone = it.phone, email = it.email, isActive = it.isActive
            )
        } else response.body()
    }

    suspend fun save(type: DirectoryType, entry: DirectoryEntry) {
        if (type == DirectoryType.EMPLOYEES) {
            write(type.path, entry.id, EmployeeRequest(entry.name, entry.position, entry.department, entry.phone, entry.email))
        } else {
            write(type.path, entry.id, ReferenceRequest(
                entry.name, entry.description, entry.building, entry.roomNumber, entry.categoryId, entry.manufacturerId
            ))
        }
    }

    private suspend inline fun <reified T : Any> write(path: String, id: Long?, request: T) {
        val response = if (id == null) {
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } else {
            client.put("$path/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        response.requireSuccess("Не удалось сохранить запись")
    }

    private fun HttpResponse.requireSuccess(fallback: String) {
        if (status.value in 200..299) return
        val message = when (status.value) {
            400 -> "Проверьте заполненные поля"
            401 -> "Сессия истекла. Войдите снова."
            403 -> "Нужны права администратора"
            404 -> "Запись не найдена"
            409 -> "Такая запись уже существует"
            else -> fallback
        }
        throw ApiException(status.value, message, "")
    }
}
