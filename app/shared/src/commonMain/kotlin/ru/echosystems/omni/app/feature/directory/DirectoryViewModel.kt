package ru.echosystems.omni.app.feature.directory

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.echosystems.omni.app.core.network.ApiException

internal data class DirectoryState(
    val type: DirectoryType? = null,
    val entries: List<DirectoryEntry> = emptyList(),
    val categories: List<DirectoryEntry> = emptyList(),
    val manufacturers: List<DirectoryEntry> = emptyList(),
    val draft: DirectoryEntry? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val original: DirectoryEntry? = null,
    val validationAttempted: Boolean = false
) {
    val dirty: Boolean get() = draft != null && draft.cleaned() != original?.cleaned()
}

internal class DirectoryViewModel(
    private val repository: DirectoryRepository,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(DirectoryState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun open(type: DirectoryType) {
        loadJob?.cancel()
        mutableState.value = DirectoryState(type = type, loading = true)
        loadJob = scope.launch {
            try {
                mutableState.value = coroutineScope {
                    val entries = async { repository.list(type) }
                    if (type == DirectoryType.MODELS) {
                        val categories = async { repository.list(DirectoryType.CATEGORIES) }
                        val manufacturers = async { repository.list(DirectoryType.MANUFACTURERS) }
                        DirectoryState(type, entries.await(), categories.await(), manufacturers.await())
                    } else DirectoryState(type, entries.await())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = DirectoryState(type = type, error = message(error, "Не удалось загрузить справочник"))
            }
        }
    }

    fun select(entry: DirectoryEntry) {
        mutableState.value = mutableState.value.copy(draft = entry, original = entry, error = null, validationAttempted = false)
    }

    fun change(entry: DirectoryEntry) {
        if (!mutableState.value.saving) mutableState.value = mutableState.value.copy(draft = entry, error = null)
    }

    fun back() {
        val current = mutableState.value
        if (current.saving) return
        if (current.draft != null) {
            mutableState.value = current.copy(draft = null, original = null, error = null, validationAttempted = false)
        } else {
            loadJob?.cancel()
            mutableState.value = DirectoryState()
        }
    }

    fun save() {
        val current = mutableState.value
        val type = current.type ?: return
        val draft = current.draft ?: return
        if (current.saving) return
        val request = draft.cleaned()
        val maxName = if (type == DirectoryType.EMPLOYEES || type == DirectoryType.MODELS) 150 else 100
        val validation = when {
            request.name.length !in 1..maxName -> "Заполните название или ФИО (до $maxName символов)"
            type == DirectoryType.MODELS && (request.categoryId == null || request.manufacturerId == null) ->
                "Выберите категорию и производителя"
            type == DirectoryType.LOCATIONS &&
                (request.building.orEmpty().length > 100 || request.roomNumber.orEmpty().length > 30) ->
                "Корпус должен быть до 100 символов, кабинет — до 30"
            type == DirectoryType.EMPLOYEES && (
                request.position.orEmpty().length > 100 || request.department.orEmpty().length > 100 ||
                    request.phone.orEmpty().length > 30 || request.email.orEmpty().length > 150
                ) -> "Сократите должность, подразделение, телефон или почту"
            else -> null
        }
        if (validation != null) {
            mutableState.value = current.copy(error = validation, validationAttempted = true)
            return
        }
        mutableState.value = current.copy(saving = true, error = null)
        scope.launch {
            try {
                repository.save(type, request)
                open(type)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = current.copy(error = message(error, "Не удалось сохранить запись"))
            }
        }
    }

    private fun message(error: Exception, fallback: String) = (error as? ApiException)?.userMessage ?: fallback
}

private fun DirectoryEntry.cleaned() = copy(
    name = name.trim(),
    description = description.clean(),
    building = building.orEmpty().trim(),
    roomNumber = roomNumber.orEmpty().trim(),
    position = position.clean(),
    department = department.clean(),
    phone = phone.clean(),
    email = email.clean()
)

private fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }
