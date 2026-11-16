package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import ru.echosystems.omni.app.feature.directory.DirectoryType
import ru.echosystems.omni.app.feature.directory.DirectoryRepository
import ru.echosystems.omni.app.feature.directory.DirectoryScreen
import ru.echosystems.omni.app.feature.directory.DirectoryViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentFormScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentFormViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentRepository
import ru.echosystems.omni.app.feature.equipment.EquipmentMoveScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentMoveViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentDetailScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentDetailViewModel
import ru.echosystems.omni.app.feature.equipment.EquipmentScreen
import ru.echosystems.omni.app.feature.equipment.EquipmentViewModel
import ru.echosystems.omni.app.feature.equipment.equipmentStatuses
import ru.echosystems.omni.app.feature.reports.ReportFilters
import ru.echosystems.omni.app.feature.reports.ReportsRepository
import ru.echosystems.omni.app.feature.reports.ReportsScreen
import ru.echosystems.omni.app.feature.reports.ReportsViewModel
import ru.echosystems.omni.app.feature.users.UsersRepository
import ru.echosystems.omni.app.feature.users.UsersScreen
import ru.echosystems.omni.app.feature.users.UsersViewModel

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
    val equipmentViewModel = remember(repository, state.user?.userId) { EquipmentViewModel(repository) }
    DisposableEffect(equipmentViewModel) {
        onDispose { equipmentViewModel.close() }
    }
    val equipmentState by equipmentViewModel.state.collectAsState()
    var selectedEquipmentId by remember { mutableStateOf<Long?>(null) }
    var reportFilters by remember { mutableStateOf<ReportFilters?>(null) }
    val filters = reportFilters
    var showDirectory by remember { mutableStateOf(false) }
    var initialDirectory by remember { mutableStateOf<DirectoryType?>(null) }
    var showUsers by remember { mutableStateOf(false) }
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
            showUsers && canEdit -> {
                val scope = rememberCoroutineScope()
                val users = remember(apiClient, scope) { UsersViewModel(UsersRepository(apiClient), scope) }
                LaunchedEffect(users) { users.load() }
                val usersState by users.state.collectAsState()
                UsersScreen(
                    usersState, requireNotNull(state.user).userId, users::edit,
                    users::change, users::save, users::load,
                    onBack = { showUsers = false }
                )
            }
            filters != null -> {
                val scope = rememberCoroutineScope()
                val reports = remember(apiClient, filters) { ReportsViewModel(ReportsRepository(apiClient), scope, filters) }
                LaunchedEffect(reports) { reports.load() }
                val reportsState by reports.state.collectAsState()
                ReportsScreen(
                    state = reportsState,
                    filters = reports.filters,
                    onRefresh = reports::load,
                    onExport = reports::export,
                    onBack = { reportFilters = null }
                )
            }
            showDirectory && canEdit -> {
                val scope = rememberCoroutineScope()
                val directory = remember(apiClient, scope) {
                    DirectoryViewModel(DirectoryRepository(apiClient), scope)
                }
                LaunchedEffect(directory) { initialDirectory?.let(directory::open) }
                val directoryState by directory.state.collectAsState()
                DirectoryScreen(
                    directoryState, directory::open, directory::select, directory::save,
                    onBack = {
                        if (directoryState.type == null) {
                            showDirectory = false
                            initialDirectory = null
                            equipmentViewModel.loadFilters()
                        } else directory.back()
                    }
                )
            }
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
                onDirectory = if (canEdit) ({ showDirectory = true }) else null,
                onEmployees = if (canEdit) ({ initialDirectory = DirectoryType.EMPLOYEES; showDirectory = true }) else null,
                onPlaces = if (canEdit) ({ initialDirectory = DirectoryType.LOCATIONS; showDirectory = true }) else null,
                onUsers = if (canEdit) ({ showUsers = true }) else null,
                onReports = {
                    val status = equipmentState.statusIndex?.let { equipmentStatuses.getOrNull(it.toInt()) }
                    reportFilters = ReportFilters(
                        search = equipmentState.search,
                        status = status?.first,
                        categoryId = equipmentState.categoryId,
                        locationId = equipmentState.locationId,
                        labels = buildList {
                            equipmentState.search.trim().takeIf { it.isNotEmpty() }?.let { add("Поиск: $it") }
                            status?.let { add("Статус: ${it.second}") }
                            equipmentState.categoryId?.let { id ->
                                add("Категория: ${equipmentState.categories.find { it.id == id }?.name ?: id}")
                            }
                            equipmentState.locationId?.let { id ->
                                add("Расположение: ${equipmentState.locations.find { it.id == id }?.name ?: id}")
                            }
                        }
                    )
                },
                onLogout = {
                    token = null
                    viewModel.logout()
                },
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
                    onChangeStatus = if (canMove) ({ code -> detail.changeStatus(code) }) else null,
                    onEdit = if (canEdit) ({ showForm = true }) else null,
                    onMove = if (canMove) ({ showMove = true }) else null
                )
            }
        }
    }
}
