package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.echosystems.omni.app.core.network.ApiClient
import ru.echosystems.omni.app.core.network.defaultApiBaseUrl
import ru.echosystems.omni.app.feature.auth.AuthRepository
import ru.echosystems.omni.app.feature.auth.AuthScreen
import ru.echosystems.omni.app.feature.auth.AuthViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentFormScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentFormViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentRepository
import ru.echosystems.omni.app.feature.equipment.EquipmentMoveScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentMoveViewModel
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
    val repository = remember(apiClient) { EquipmentRepository(apiClient) }
    val equipmentViewModel = remember(repository) { EquipmentViewModel(repository) }
    val equipmentState by equipmentViewModel.state.collectAsState()
    var selectedEquipmentId by remember { mutableStateOf<Long?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    val canEdit = state.user?.role == "admin"
    val canMove = canEdit || state.user?.role == "operator"
    val equipmentId = selectedEquipmentId

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.user == null -> AuthScreen(
                state, viewModel::onUsernameChanged, viewModel::onPasswordChanged, viewModel::login
            )
            showMove && canMove && equipmentId != null -> {
                val scope = rememberCoroutineScope()
                val move = remember(repository, equipmentId) {
                    EquipmentMoveViewModel(repository, equipmentId, scope)
                }
                LaunchedEffect(move) { move.load() }
                val moveState by move.state.collectAsState()
                EquipmentMoveScreen(
                    state = moveState,
                    onChange = move::change,
                    onRetry = move::load,
                    onSave = { move.save { showMove = false } },
                    onBack = { showMove = false }
                )
            }
            showForm && canEdit -> {
                val scope = rememberCoroutineScope()
                val form = remember(repository, equipmentId) {
                    EquipmentFormViewModel(repository, equipmentId, scope)
                }
                val formState by form.state.collectAsState()
                EquipmentFormScreen(
                    editing = equipmentId != null,
                    state = formState,
                    onChange = form::change,
                    onRetry = form::load,
                    onSave = {
                        form.save { savedId ->
                            selectedEquipmentId = savedId
                            showForm = false
                        }
                    },
                    onBack = { showForm = false }
                )
            }
            equipmentId == null -> EquipmentScreen(
                state = equipmentState,
                onSearchChanged = equipmentViewModel::onSearchChanged,
                onRefresh = equipmentViewModel::load,
                onLoadMore = equipmentViewModel::loadMore,
                onStatus = equipmentViewModel::selectStatus,
                onCategory = equipmentViewModel::selectCategory,
                onLocation = equipmentViewModel::selectLocation,
                onAdd = if (canEdit) ({ showForm = true }) else null,
                onItemClick = { selectedEquipmentId = it }
            )
            else -> {
                val scope = rememberCoroutineScope()
                val detail = remember(repository, equipmentId) { EquipmentDetailViewModel(repository, scope) }
                val detailState by detail.state.collectAsState()
                EquipmentDetailScreen(
                    equipmentId = equipmentId,
                    state = detailState,
                    onLoad = detail::load,
                    onBack = { selectedEquipmentId = null },
                    onChangeStatus = { detail.changeStatus(it) },
                    onEdit = if (canEdit) ({ showForm = true }) else null,
                    onMove = if (canMove) ({ showMove = true }) else null
                )
            }
        }
    }
}
