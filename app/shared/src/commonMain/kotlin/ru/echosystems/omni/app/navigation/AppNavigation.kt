package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.defaultApiBaseUrl
import ru.echosystems.omni.app.feature.auth.AuthRepository
import ru.echosystems.omni.app.feature.auth.AuthScreen
import ru.echosystems.omni.app.feature.auth.AuthViewModel

@Composable
fun AppNavigation() {
    val apiClient = remember { ApiClient(defaultApiBaseUrl()) }
    val viewModel = remember { AuthViewModel(AuthRepository(apiClient)) }
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        AuthScreen(
            state = state,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onLogin = viewModel::login
        )
    }
}
