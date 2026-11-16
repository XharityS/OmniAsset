package ru.echosystems.omni.app.feature.reports

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.Serializable
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.ApiException

@Serializable
internal data class StatusSummary(val statusCode: String, val statusName: String, val amount: Long)

internal data class ReportFilters(
    val search: String,
    val status: String?,
    val categoryId: Long?,
    val locationId: Long?,
    val labels: List<String>
)

internal class ReportsRepository(apiClient: ApiClient) {
    private val client = apiClient.httpClient

    suspend fun summary(): List<StatusSummary> {
        val response = client.get("/api/summary/statuses")
        response.requireSuccess("Не удалось загрузить сводку")
        return response.body()
    }

    suspend fun downloadCsv(filters: ReportFilters, destination: CsvDestination) {
        client.prepareGet("/api/equipment.csv") {
            parameter("search", filters.search.takeIf { it.isNotBlank() })
            parameter("status", filters.status)
            parameter("categoryId", filters.categoryId)
            parameter("locationId", filters.locationId)
        }.execute { response ->
            response.requireSuccess("Не удалось выгрузить CSV")
            val contentType = response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()
            check(contentType.equals("text/csv", ignoreCase = true))
            val channel = response.bodyAsChannel()
            destination.save { write ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val received = channel.readAvailable(buffer)
                    if (received == -1) break
                    if (received > 0) write(buffer, received)
                }
                channel.closedCause?.let { throw it }
            }
        }
    }

    private fun HttpResponse.requireSuccess(fallback: String) {
        if (status.value in 200..299) return
        val message = if (status.value == 401) "Сессия истекла. Войдите снова." else fallback
        throw ApiException(status.value, message, "")
    }
}
