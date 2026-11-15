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
                    val details = async { repository.loadDetails(equipmentId) }
                    val locations = async { repository.references("locations") }
                    val departments = async { repository.references("departments") }
                    val employees = async { repository.references("employees") }
                    val item = details.await()
                    check(item.responsibleName == null || item.responsibleId != null)
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
                mutableState.value = EquipmentMoveState(loading = false, error = "Не удалось загрузить данные перемещения")
            }
        }
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
