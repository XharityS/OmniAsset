package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.defaultApiBaseUrl
import ru.echosystems.omni.app.feature.auth.AuthRepository
import ru.echosystems.omni.app.feature.auth.AuthScreen
import ru.echosystems.omni.app.feature.auth.AuthViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentRepository
import ru.echosystems.omni.app.feature.equipment.EquipmentDetailScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentDetailViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentViewModel

@Composable
fun AppNavigation() {
    var token by remember { mutableStateOf<String?>(null) }
    val apiClient = remember { ApiClient(defaultApiBaseUrl()) { token } }
    val viewModel = remember {
        AuthViewModel(
            repository = AuthRepository(apiClient),
            onAuthenticated = { response -> token = response.token }
        )
    }
    val state by viewModel.state.collectAsState()
    val equipmentViewModel = remember { EquipmentViewModel(EquipmentRepository(apiClient)) }
    val equipmentState by equipmentViewModel.state.collectAsState()
    val equipmentDetailViewModel = remember { EquipmentDetailViewModel(EquipmentRepository(apiClient)) }
    val equipmentDetailState by equipmentDetailViewModel.state.collectAsState()
    var selectedEquipmentId by remember { mutableStateOf<Long?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (state.user == null) AuthScreen(state, viewModel::onUsernameChanged, viewModel::onPasswordChanged, viewModel::login)
        else if (selectedEquipmentId == null) {
            EquipmentScreen(equipmentState, equipmentViewModel::onSearchChanged, equipmentViewModel::load) { selectedEquipmentId = it }
        } else {
            EquipmentDetailScreen(
                equipmentId = selectedEquipmentId!!,
                state = equipmentDetailState,
                onLoad = equipmentDetailViewModel::load,
                onBack = { selectedEquipmentId = null },
                onChangeStatus = equipmentDetailViewModel::changeStatus
            )
        }
    }
}
