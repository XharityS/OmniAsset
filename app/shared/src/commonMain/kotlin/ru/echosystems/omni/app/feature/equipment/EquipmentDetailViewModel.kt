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
data class EquipmentDetailUiState(
    val item: EquipmentDetails? = null,
    val history: List<EquipmentHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null
)

class EquipmentDetailViewModel(
    private val repository: EquipmentRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _state = MutableStateFlow(EquipmentDetailUiState())
    val state: StateFlow<EquipmentDetailUiState> = _state.asStateFlow()

    fun load(id: Long) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val item = repository.loadDetails(id)
                item to repository.loadHistory(id)
            }.onSuccess { (item, history) ->
                _state.value = _state.value.copy(isLoading = false, item = item, history = history)
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = message(error, "Не удалось загрузить оборудование"))
            }
        }
    }

    fun changeStatus(statusCode: String, comment: String? = null) {
        val id = _state.value.item?.id ?: return
        scope.launch {
            _state.value = _state.value.copy(isActionLoading = true, actionMessage = null)
            runCatching { repository.changeStatus(id, statusCode, comment) }
                .onSuccess { load(id); _state.value = _state.value.copy(isActionLoading = false, actionMessage = "Статус обновлён") }
                .onFailure { error -> _state.value = _state.value.copy(isActionLoading = false, actionMessage = message(error, "Не удалось изменить статус")) }
        }
    }

    private fun message(error: Throwable, fallback: String) = (error as? ApiException)?.userMessage ?: fallback
}
