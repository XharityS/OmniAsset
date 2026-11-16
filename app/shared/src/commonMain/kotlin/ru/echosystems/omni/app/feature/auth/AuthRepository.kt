package ru.echosystems.omni.app.feature.auth

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.ApiException

class AuthRepository(
    private val apiClient: ApiClient
) {
    suspend fun login(username: String, password: String): LoginResponse {
        val response = apiClient.httpClient.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }

        if (response.status.value !in 200..299) {
            val responseBody = response.bodyAsText()
            val userMessage = when (response.status.value) {
                401 -> "Неверное имя пользователя или пароль"
                429 -> "Слишком много попыток. Попробуйте позже"
                else -> "Не удалось выполнить вход. Код ошибки: ${response.status.value}"
            }
            throw ApiException(response.status.value, userMessage, responseBody)
        }

        return response.body()
    }
}
