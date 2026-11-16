package ru.echosystems.omni.app.feature.equipment

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.echosystems.omni.app.core.network.ApiException

@Serializable
data class EquipmentMoveRequest(
    val locationId: Long?,
    val departmentId: Long?,
    val employeeId: Long?,
    val comment: String? = null
)

data class EquipmentMoveState(
    val item: EquipmentDetails? = null,
    val draft: EquipmentMoveRequest? = null,
    val locations: List<ReferenceItem> = emptyList(),
    val departments: List<ReferenceItem> = emptyList(),
    val employees: List<ReferenceItem> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null
) {
    val dirty: Boolean get() = hasChanges || !draft?.comment.isNullOrBlank()

    val hasChanges: Boolean
        get() = item != null && draft != null && (
            draft.locationId != item.locationId ||
                draft.departmentId != item.departmentId ||
                draft.employeeId != item.responsibleId
            )
}

class EquipmentMoveViewModel(
    private val repository: EquipmentRepository,
    private val equipmentId: Long,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(EquipmentMoveState())
    val state = mutableState.asStateFlow()

    fun load() {
        mutableState.value = EquipmentMoveState()
        scope.launch {
            try {
                mutableState.value = coroutineScope {
                    val details = async { loadPart("карточку оборудования") { repository.loadDetails(equipmentId) } }
                    val locations = async { loadPart("справочник расположений") { repository.references("locations") } }
                    val departments = async { loadPart("справочник подразделений") { repository.references("departments") } }
                    val employees = async { loadPart("справочник сотрудников") { repository.references("employees") } }
                    val item = details.await()
                    if (item.responsibleName != null && item.responsibleId == null) {
                        throw MoveLoadException(
                            "Сервер не передал ID текущего ответственного. Обновите API, чтобы сохранить закрепление."
                        )
                    }
                    val activeEmployees = employees.await()
                    val currentEmployee = item.responsibleId?.let { id ->
                        ReferenceItem(id, item.responsibleName ?: "Текущий ответственный")
                    }
                    EquipmentMoveState(
                        item = item,
                        draft = EquipmentMoveRequest(item.locationId, item.departmentId, item.responsibleId),
                        locations = locations.await(),
                        departments = departments.await(),
                        employees = if (currentEmployee != null && activeEmployees.none { it.id == currentEmployee.id }) {
                            listOf(currentEmployee) + activeEmployees
                        } else activeEmployees,
                        loading = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = EquipmentMoveState(
                    loading = false,
                    error = (error as? MoveLoadException)?.message ?: "Не удалось загрузить данные перемещения"
                )
            }
        }
    }

    private suspend fun <T> loadPart(label: String, fetch: suspend () -> T): T = try {
        fetch()
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        val message = when (error.statusCode) {
            401 -> "Сессия истекла. Войдите снова."
            else -> "Не удалось загрузить $label (HTTP ${error.statusCode})."
        }
        throw MoveLoadException(message, error)
    } catch (error: Exception) {
        throw MoveLoadException("Не удалось загрузить $label. Проверьте подключение и ответ API.", error)
    }

    fun change(draft: EquipmentMoveRequest) {
        val current = mutableState.value
        if (!current.saving) mutableState.value = current.copy(draft = draft, error = null)
    }

    fun save(onSaved: () -> Unit) {
        val current = mutableState.value
        val draft = current.draft ?: return
        if (current.saving || !current.hasChanges) return
        mutableState.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                repository.move(equipmentId, draft.copy(comment = draft.comment?.trim()?.takeIf { it.isNotEmpty() }))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = current.copy(
                    error = (error as? ApiException)?.userMessage ?: "Не удалось сохранить перемещение"
                )
                return@launch
            }
            onSaved()
        }
    }
}

private class MoveLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
