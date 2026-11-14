package ru.echosystems.omni.app.feature.auth

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.echosystems.omni.app.core.network.ApiClient

class AuthRepository(
    private val apiClient: ApiClient
) {
    suspend fun login(username: String, password: String): LoginResponse {
        return apiClient.httpClient.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body()
    }
}
