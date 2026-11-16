package ru.echosystems.omni.app.feature.reports

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.echosystems.omni.app.core.network.ApiException

internal data class ReportsState(
    val statuses: List<StatusSummary> = emptyList(),
    val loading: Boolean = false,
    val loadingError: String? = null,
    val exporting: Boolean = false,
    val exportError: String? = null,
    val exported: Boolean = false
) {
    val total: Long get() = statuses.sumOf { it.amount }
}

internal class ReportsViewModel(
    private val repository: ReportsRepository,
    private val scope: CoroutineScope,
    val filters: ReportFilters
) {
    private val mutableState = MutableStateFlow(ReportsState())
    val state = mutableState.asStateFlow()

    fun load() {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, loadingError = null) }
        scope.launch {
            try {
                val statuses = repository.summary()
                mutableState.update { it.copy(statuses = statuses, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(loading = false, loadingError = message(error, "Не удалось загрузить сводку"))
                }
            }
        }
    }

    fun export(destination: CsvDestination?) {
        if (destination == null || mutableState.value.exporting) return
        mutableState.update { it.copy(exporting = true, exportError = null, exported = false) }
        scope.launch {
            try {
                repository.downloadCsv(filters, destination)
                mutableState.update { it.copy(exporting = false, exported = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(exporting = false, exportError = message(error, "Не удалось сохранить CSV"))
                }
            }
        }
    }

    private fun message(error: Exception, fallback: String) =
        (error as? ApiException)?.userMessage ?: fallback
}
