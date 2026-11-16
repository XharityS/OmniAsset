package ru.echosystems.omni.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun platformHttpClient(): HttpClient

class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String? = { null }
) {
    val httpClient: HttpClient = platformHttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url(baseUrl)
            tokenProvider()?.let { headers.append("Authorization", "Bearer $it") }
        }
    }
}
