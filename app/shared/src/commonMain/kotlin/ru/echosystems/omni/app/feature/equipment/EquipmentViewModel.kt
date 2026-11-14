package ru.echosystems.omni.app.feature.equipment

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.echosystems.omni.app.core.network.ApiException

@Immutable
data class EquipmentUiState(
    val search: String = "",
    val items: List<EquipmentRow> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class EquipmentViewModel(
    private val repository: EquipmentRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _state = MutableStateFlow(EquipmentUiState())
    val state: StateFlow<EquipmentUiState> = _state.asStateFlow()

    fun onSearchChanged(value: String) { _state.value = _state.value.copy(search = value) }

    fun load() {
        val current = _state.value
        scope.launch {
            _state.value = current.copy(isLoading = true, errorMessage = null)
            runCatching { repository.loadPage(current.search) }
                .onSuccess { page -> _state.value = _state.value.copy(isLoading = false, items = page.items) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = (error as? ApiException)?.userMessage ?: "Не удалось загрузить технику"
                    )
                }
        }
    }
}
