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
data class EquipmentFormRequest(
    val inventoryNumber: String = "",
    val serialNumber: String? = null,
    val modelId: Long? = null,
    val statusId: Long? = null,
    val locationId: Long? = null,
    val departmentId: Long? = null,
    val purchaseDate: String? = null,
    val commissioningDate: String? = null,
    val warrantyUntil: String? = null,
    val purchasePrice: String? = null,
    val notes: String? = null
)

@Serializable
internal data class CreatedEquipment(val id: Long)

data class EquipmentFormState(
    val draft: EquipmentFormRequest = EquipmentFormRequest(),
    val models: List<ReferenceItem> = emptyList(),
    val statuses: List<ReferenceItem> = emptyList(),
    val locations: List<ReferenceItem> = emptyList(),
    val departments: List<ReferenceItem> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val ready: Boolean = false,
    val error: String? = null
)

class EquipmentFormViewModel(
    private val repository: EquipmentRepository,
    private val id: Long?,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(EquipmentFormState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.value = EquipmentFormState()
        scope.launch {
            try {
                mutableState.value = coroutineScope {
                    val models = async { repository.references("models") }
                    val statuses = async { repository.references("statuses") }
                    val locations = async { repository.references("locations") }
                    val departments = async { repository.references("departments") }
                    val item = id?.let { repository.loadDetails(it) }
                    val draft = item?.let {
                        EquipmentFormRequest(
                            it.inventoryNumber, it.serialNumber, it.modelId, it.statusId,
                            it.locationId, it.departmentId, it.purchaseDate, it.commissioningDate,
                            it.warrantyUntil, it.purchasePrice, it.notes
                        )
                    } ?: EquipmentFormRequest()
                    EquipmentFormState(
                        draft, models.await(), statuses.await(), locations.await(), departments.await(),
                        loading = false, ready = true
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = EquipmentFormState(loading = false, error = "Не удалось загрузить форму")
            }
        }
    }

    fun change(draft: EquipmentFormRequest) {
        if (!mutableState.value.saving) {
            mutableState.value = mutableState.value.copy(draft = draft, error = null)
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = mutableState.value
        if (!current.ready || current.saving) return
        val draft = current.draft
        val request = draft.copy(
            inventoryNumber = draft.inventoryNumber.trim(),
            serialNumber = draft.serialNumber.clean(),
            purchaseDate = draft.purchaseDate.clean(),
            commissioningDate = draft.commissioningDate.clean(),
            warrantyUntil = draft.warrantyUntil.clean(),
            purchasePrice = draft.purchasePrice.clean()?.replace(',', '.'),
            notes = draft.notes.clean()
        )
        val error = when {
            request.inventoryNumber.length !in 1..50 -> "Укажите инвентарный номер до 50 символов"
            (request.serialNumber?.length ?: 0) > 100 -> "Серийный номер должен быть до 100 символов"
            request.modelId == null || request.statusId == null -> "Выберите модель и статус"
            listOfNotNull(request.purchaseDate, request.commissioningDate, request.warrantyUntil)
                .any { !it.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) } -> "Введите даты в формате ГГГГ-ММ-ДД"
            request.purchasePrice?.matches(Regex("[0-9]{1,10}(\\.[0-9]{1,2})?")) == false -> "Введите корректную стоимость"
            else -> null
        }
        if (error != null) {
            mutableState.value = current.copy(error = error)
            return
        }
        mutableState.value = current.copy(saving = true, error = null)
        scope.launch {
            val savedId = try {
                repository.save(id, request)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = current.copy(
                    error = (error as? ApiException)?.userMessage ?: "Не удалось сохранить оборудование"
                )
                return@launch
            }
            onSaved(savedId)
        }
    }

    private fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }
}
