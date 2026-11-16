package ru.echosystems.omni.app.feature.users

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.ApiException

internal enum class UserRole(val code: String, val label: String) {
    ADMIN("admin", "Администратор"), OPERATOR("operator", "Оператор"), VIEWER("viewer", "Наблюдатель");

    companion object {
        fun label(code: String) = entries.firstOrNull { it.code == code }?.label ?: code
    }
}

@Serializable
internal data class ManagedUser(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String? = null,
    val role: String,
    val isActive: Boolean
)

internal data class UserDraft(
    val id: String? = null,
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "operator",
    val active: Boolean = true,
    val password: String = ""
)

@Serializable
private data class CreateUser(val username: String, val password: String, val fullName: String, val email: String?, val role: String)

@Serializable
private data class UpdateUser(val fullName: String, val email: String?, val role: String, val isActive: Boolean)

internal class UsersRepository(api: ApiClient) {
    private val client = api.httpClient

    suspend fun list(): List<ManagedUser> =
        client.get("/api/admin/users").also { it.checkStatus() }.body()

    suspend fun save(draft: UserDraft) {
        val email = draft.email.ifBlank { null }
        val response = if (draft.id == null) {
            client.post("/api/admin/users") {
                contentType(ContentType.Application.Json)
                setBody(CreateUser(draft.username, draft.password, draft.fullName, email, draft.role))
            }
        } else {
            client.put("/api/admin/users/${draft.id}") {
                contentType(ContentType.Application.Json)
                setBody(UpdateUser(draft.fullName, email, draft.role, draft.active))
            }
        }
        response.checkStatus()
    }

    private fun HttpResponse.checkStatus() {
        if (status.value in 200..299) return
        val message = when (status.value) {
            400 -> "Проверьте заполненные поля"
            401 -> "Сессия истекла. Войдите снова."
            403 -> "Недостаточно прав для этого действия"
            404 -> "Запись или маршрут API не найден"
            409 -> "Такой логин или адрес почты уже используется"
            else -> "Не удалось выполнить запрос"
        }
        throw ApiException(status.value, message, "")
    }
}

internal data class UsersState(
    val users: List<ManagedUser> = emptyList(),
    val draft: UserDraft? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null
)

internal class UsersViewModel(private val repository: UsersRepository, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(UsersState())
    val state = mutableState.asStateFlow()

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        scope.launch {
            try {
                val users = repository.list()
                mutableState.update { it.copy(users = users, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = message(error)) }
            }
        }
    }

    fun edit(user: ManagedUser?) {
        change(user?.let { UserDraft(it.id, it.username, it.fullName, it.email.orEmpty(), it.role, it.isActive) } ?: UserDraft())
    }

    fun change(draft: UserDraft?) {
        if (!mutableState.value.saving) mutableState.update { it.copy(draft = draft, error = null) }
    }

    fun save() {
        val current = mutableState.value
        val draft = current.draft ?: return
        if (current.saving) return
        val valid = draft.copy(
            username = draft.username.trim(), fullName = draft.fullName.trim(), email = draft.email.trim()
        )
        val error = when {
            valid.fullName.length !in 1..150 -> "Укажите ФИО (до 150 символов)"
            valid.email.length > 150 -> "Сократите адрес почты до 150 символов"
            valid.id == null && valid.username.length !in 1..50 -> "Укажите логин (до 50 символов)"
            valid.id == null && (valid.password.length < 8 || valid.password.encodeToByteArray().size > 72) ->
                "Пароль должен быть не короче 8 символов и не длиннее 72 байт"
            else -> null
        }
        if (error != null) {
            mutableState.update { it.copy(error = error) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        scope.launch {
            try {
                repository.save(valid)
                mutableState.update { it.copy(draft = null, saving = false) }
                load()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.update { it.copy(saving = false, error = message(error)) }
            }
        }
    }

    private fun message(error: Exception) = (error as? ApiException)?.userMessage ?: "Не удалось выполнить запрос"
}
