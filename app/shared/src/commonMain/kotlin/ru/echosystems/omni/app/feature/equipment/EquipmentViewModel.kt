package ru.echosystems.omni.app.feature.equipment

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.echosystems.omni.app.core.network.ApiException

data class EquipmentUiState(
    val search: String = "",
    val items: List<EquipmentRow> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null
)

class EquipmentViewModel(
    private val repository: EquipmentRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    private val _state = MutableStateFlow(EquipmentUiState())
    val state: StateFlow<EquipmentUiState> = _state.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var nextOffset = 0L

    fun onSearchChanged(value: String) {
        _state.update { it.copy(search = value) }
        request(reset = true, debounce = true)
    }

    fun load() = request(reset = true)
    fun loadMore() {
        if (_state.value.hasMore && !_state.value.isLoading && !_state.value.isLoadingMore) request(reset = false)
    }

    private fun request(reset: Boolean, debounce: Boolean = false) {
        job?.cancel()
        val requestGeneration = ++generation
        val query = _state.value.search
        val offset = if (reset) 0L else nextOffset
        _state.update { it.copy(isLoading = reset, isLoadingMore = !reset, errorMessage = null) }
        job = scope.launch {
            try {
                if (debounce) delay(350)
                val page = repository.loadPage(query, offset)
                if (requestGeneration != generation) return@launch
                nextOffset = page.offset + page.items.size
                _state.update { it.copy(
                    items = if (reset) page.items else (it.items + page.items).distinctBy { row -> row.id },
                    hasMore = page.hasMore, isLoading = false, isLoadingMore = false
                ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestGeneration == generation) _state.update { it.copy(
                    isLoading = false, isLoadingMore = false,
                    errorMessage = (error as? ApiException)?.userMessage ?: "Не удалось загрузить технику. Повторите попытку."
                ) }
            }
        }
    }

    fun close() { scope.cancel() }
}
