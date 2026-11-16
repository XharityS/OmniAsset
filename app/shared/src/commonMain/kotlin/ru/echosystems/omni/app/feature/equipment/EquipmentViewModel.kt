package ru.echosystems.omni.app.feature.equipment

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.echosystems.omni.app.core.network.ApiException

data class EquipmentUiState(
    val search: String = "",
    val categoryId: Long? = null,
    val statusIndex: Long? = null,
    val locationId: Long? = null,
    val categories: List<ReferenceItem> = emptyList(),
    val locations: List<ReferenceItem> = emptyList(),
    val filterError: String? = null,
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

    fun load() {
        request(reset = true)
        if (_state.value.categories.isEmpty() || _state.value.locations.isEmpty()) loadFilters()
    }

    private var filterJob: Job? = null

    fun loadFilters() {
        if (filterJob?.isActive == true) return
        filterJob = scope.launch {
            try {
                val categories = repository.references("categories")
                val locations = repository.references("locations")
                _state.update { it.copy(categories = categories, locations = locations, filterError = null) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(filterError = "Не удалось загрузить фильтры. Нажмите обновление.") }
            }
        }
    }

    fun selectStatus(id: Long?) {
        _state.update { it.copy(statusIndex = id) }
        request(reset = true)
    }

    fun selectCategory(id: Long?) {
        _state.update { it.copy(categoryId = id) }
        request(reset = true)
    }

    fun selectLocation(id: Long?) {
        _state.update { it.copy(locationId = id) }
        request(reset = true)
    }
    fun loadMore() {
        if (_state.value.hasMore && !_state.value.isLoading && !_state.value.isLoadingMore) request(reset = false)
    }

    private fun request(reset: Boolean, debounce: Boolean = false) {
        job?.cancel()
        val requestGeneration = ++generation
        val filters = _state.value
        val query = filters.search
        val offset = if (reset) 0L else nextOffset
        _state.update { it.copy(isLoading = reset, isLoadingMore = !reset, errorMessage = null) }
        job = scope.launch {
            try {
                if (debounce) delay(350)
                val page = repository.loadPage(query, offset, filters.categoryId, filters.locationId, filters.statusIndex?.let { equipmentStatuses.getOrNull(it.toInt())?.first })
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
