package ru.echosystems.omni.app.feature.auth

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.echosystems.omni.app.core.network.ApiException

@Immutable
data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: LoginResponse? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val onAuthenticated: (LoginResponse) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onUsernameChanged(value: String) {
        _state.value = _state.value.copy(username = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _state.value = _state.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val current = _state.value
        if (current.isLoading || current.user != null) return
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.value = current.copy(errorMessage = "Введите имя пользователя и пароль")
            return
        }

        _state.value = current.copy(isLoading = true, errorMessage = null)
        scope.launch {
            runCatching {
                repository.login(current.username.trim(), current.password)
            }.onSuccess { response ->
                onAuthenticated(response)
                _state.value = _state.value.copy(password = "", isLoading = false, user = response)
            }.onFailure { error ->
                println("OmniAsset login failed: ${error.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = when (error) {
                        is ApiException -> error.userMessage
                        else -> "Не удалось подключиться к серверу"
                    }
                )
            }
        }
    }

    fun logout() { _state.value = AuthUiState() }

    fun close() {
        scope.coroutineContext.cancel()
    }
}
