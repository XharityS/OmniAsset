package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
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
import ru.echosystems.omni.app.feature.equipment.EquipmentUiState
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
    var section by remember { mutableStateOf(WorkspaceSection.EQUIPMENT) }
    var navigationBusy by remember { mutableStateOf(false) }
    val guard = remember { WorkspaceLeaveGuard() }
    var pendingTransition by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    val user = state.user
    val canEdit = user?.role == "admin"
    val canMove = canEdit || user?.role == "operator"
    val equipmentId = selectedEquipmentId
    val filters = reportFilters
    val directoryType = when (section) {
        WorkspaceSection.EMPLOYEES -> DirectoryType.EMPLOYEES
        WorkspaceSection.PLACES -> DirectoryType.LOCATIONS
        else -> null
    }

    fun navigate(destination: WorkspaceSection) {
        if (navigationBusy || (destination.adminOnly && !canEdit)) return
        if (destination == section && (destination != WorkspaceSection.EQUIPMENT ||
            (selectedEquipmentId == null && !showForm && !showMove))
        ) return
        if (section in listOf(WorkspaceSection.DIRECTORY, WorkspaceSection.EMPLOYEES, WorkspaceSection.PLACES)) {
            equipmentViewModel.loadFilters()
        }
        reportFilters = if (destination == WorkspaceSection.REPORTS) equipmentState.toReportFilters() else null
        selectedEquipmentId = null
        showForm = false
        showMove = false
        section = destination
    }

    fun requestLeave(action: () -> Unit) {
        if (navigationBusy || pendingTransition != null) return
        if (guard.dirty()) pendingTransition = action else action()
    }

    if (user == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AuthScreen(state, viewModel::onUsernameChanged, viewModel::onPasswordChanged, viewModel::login)
        }
        return
    }

    Workspace(
        selected = section,
        isAdmin = canEdit,
        navigationEnabled = !navigationBusy,
        onSelect = { destination ->
            if (destination != section || destination == WorkspaceSection.EQUIPMENT) {
                requestLeave { navigate(destination) }
            }
        },
        onLogout = {
            requestLeave {
                navigate(WorkspaceSection.EQUIPMENT)
                token = null
                viewModel.logout()
            }
        }
    ) { wide ->
        when {
            section == WorkspaceSection.USERS && canEdit -> {
                val scope = rememberCoroutineScope()
                val users = remember(apiClient, scope) { UsersViewModel(UsersRepository(apiClient), scope) }
                LaunchedEffect(users) { users.load() }
                val usersState by users.state.collectAsState()
                SideEffect {
                    navigationBusy = usersState.saving
                    guard.dirty = { users.state.value.dirty }
                }
                val back: () -> Unit = {
                    if (!users.state.value.saving) requestLeave {
                        if (users.state.value.draft != null) users.change(null)
                        else navigate(WorkspaceSection.EQUIPMENT)
                    }
                }
                WorkspaceBackHandler(back)
                UsersScreen(
                    usersState, user.userId, users::edit,
                    users::change, users::save, users::load,
                    onBack = back
                )
            }
            section == WorkspaceSection.REPORTS && filters != null -> {
                val scope = rememberCoroutineScope()
                val reports = remember(apiClient, filters) { ReportsViewModel(ReportsRepository(apiClient), scope, filters) }
                LaunchedEffect(reports) { reports.load() }
                val reportsState by reports.state.collectAsState()
                SideEffect {
                    navigationBusy = reportsState.exporting
                    guard.dirty = { false }
                }
                WorkspaceBackHandler {
                    if (!reports.state.value.exporting) navigate(WorkspaceSection.EQUIPMENT)
                }
                ReportsScreen(
                    state = reportsState,
                    filters = reports.filters,
                    onRefresh = reports::load,
                    onExport = reports::export,
                    onBack = { navigate(WorkspaceSection.EQUIPMENT) }
                )
            }
            canEdit && (section == WorkspaceSection.DIRECTORY || directoryType != null) -> {
                val scope = rememberCoroutineScope()
                val directory = remember(apiClient, scope, section) {
                    DirectoryViewModel(DirectoryRepository(apiClient), scope)
                }
                LaunchedEffect(directory) { directoryType?.let(directory::open) }
                val directoryState by directory.state.collectAsState()
                SideEffect {
                    navigationBusy = directoryState.saving
                    guard.dirty = { directory.state.value.dirty }
                }
                val back: () -> Unit = {
                    val current = directory.state.value
                    val atEntry = current.type == null ||
                        (directoryType == current.type && current.draft == null)
                    if (!current.saving) requestLeave {
                        if (atEntry) navigate(WorkspaceSection.EQUIPMENT) else directory.back()
                    }
                }
                WorkspaceBackHandler(back)
                DirectoryScreen(
                    directoryState, directory::open, directory::select, directory::change, directory::save, onBack = back
                )
            }
            showMove && canMove && equipmentId != null -> {
                val scope = rememberCoroutineScope()
                val move = remember(repository, equipmentId) {
                    EquipmentMoveViewModel(repository, equipmentId, scope)
                }
                LaunchedEffect(move) { move.load() }
                val moveState by move.state.collectAsState()
                SideEffect {
                    navigationBusy = moveState.saving
                    guard.dirty = { move.state.value.dirty }
                }
                val back: () -> Unit = {
                    if (!move.state.value.saving) requestLeave { showMove = false }
                }
                WorkspaceBackHandler(back)
                EquipmentMoveScreen(
                    state = moveState,
                    onChange = move::change,
                    onRetry = move::load,
                    onSave = { move.save { navigationBusy = false; showMove = false } },
                    onBack = back
                )
            }
            showForm && canEdit -> {
                val scope = rememberCoroutineScope()
                val form = remember(repository, equipmentId) {
                    EquipmentFormViewModel(repository, equipmentId, scope)
                }
                val formState by form.state.collectAsState()
                SideEffect {
                    navigationBusy = formState.saving
                    guard.dirty = { form.state.value.dirty }
                }
                val back: () -> Unit = {
                    if (!form.state.value.saving) requestLeave { showForm = false }
                }
                WorkspaceBackHandler(back)
                EquipmentFormScreen(
                    editing = equipmentId != null,
                    state = formState,
                    onChange = form::change,
                    onRetry = form::load,
                    onSave = {
                        form.save { savedId ->
                            navigationBusy = false
                            selectedEquipmentId = savedId
                            showForm = false
                        }
                    },
                    onBack = back
                )
            }
            equipmentId == null -> {
                SideEffect { navigationBusy = false; guard.dirty = { false } }
                EquipmentScreen(
                    state = equipmentState,
                    wide = wide,
                    onSearchChanged = equipmentViewModel::onSearchChanged,
                    onRefresh = equipmentViewModel::load,
                    onLoadMore = equipmentViewModel::loadMore,
                    onStatus = equipmentViewModel::selectStatus,
                    onCategory = equipmentViewModel::selectCategory,
                    onLocation = equipmentViewModel::selectLocation,
                    onAdd = if (canEdit) ({ showForm = true }) else null,
                    onItemClick = { selectedEquipmentId = it }
                )
            }
            else -> {
                val scope = rememberCoroutineScope()
                val detail = remember(repository, equipmentId) { EquipmentDetailViewModel(repository, scope) }
                val detailState by detail.state.collectAsState()
                SideEffect {
                    navigationBusy = detailState.isActionLoading
                    guard.dirty = { false }
                }
                WorkspaceBackHandler { if (!detail.state.value.isActionLoading) selectedEquipmentId = null }
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
    val transition = pendingTransition
    if (transition != null) {
        AlertDialog(
            onDismissRequest = { pendingTransition = null },
            title = { Text("Несохранённые изменения") },
            text = { Text("Если уйти с этого экрана, изменения будут потеряны.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingTransition = null
                    transition()
                }) { Text("Уйти без сохранения") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTransition = null }) { Text("Продолжить редактирование") }
            }
        )
    }
}

private fun EquipmentUiState.toReportFilters(): ReportFilters {
    val status = statusIndex?.let { equipmentStatuses.getOrNull(it.toInt()) }
    return ReportFilters(
        search = search,
        status = status?.first,
        categoryId = categoryId,
        locationId = locationId,
        labels = buildList {
            search.trim().takeIf { it.isNotEmpty() }?.let { add("Поиск: $it") }
            status?.let { add("Статус: ${it.second}") }
            categoryId?.let { id -> add("Категория: ${categories.find { it.id == id }?.name ?: id}") }
            locationId?.let { id -> add("Расположение: ${locations.find { it.id == id }?.name ?: id}") }
        }
    )
}

private class WorkspaceLeaveGuard {
    var dirty: () -> Boolean = { false }
}
