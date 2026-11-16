package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChartNoAxesCombined
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.User

internal enum class WorkspaceSection(
    val title: String,
    val icon: ImageVector,
    val adminOnly: Boolean = false
) {
    EQUIPMENT("Техника", Lucide.Package),
    EMPLOYEES("Сотрудники", Lucide.User, adminOnly = true),
    PLACES("Места", Lucide.MapPin, adminOnly = true),
    DIRECTORY("Справочники", Lucide.Package, adminOnly = true),
    REPORTS("Отчёты", Lucide.ChartNoAxesCombined),
    USERS("Пользователи", Lucide.User, adminOnly = true)
}

@Composable
internal fun Workspace(
    selected: WorkspaceSection,
    isAdmin: Boolean,
    navigationEnabled: Boolean,
    onSelect: (WorkspaceSection) -> Unit,
    onLogout: () -> Unit,
    content: @Composable (wide: Boolean) -> Unit
) {
    val sections = WorkspaceSection.entries.filter { isAdmin || !it.adminOnly }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val wide = maxWidth >= 900.dp
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    if (wide) {
                        WorkspaceSidebar(sections, selected, navigationEnabled, onSelect, onLogout)
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) { content(wide) }
                }
                if (!wide) {
                    WorkspaceBottomBar(sections, selected, navigationEnabled, onSelect, onLogout)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSidebar(
    sections: List<WorkspaceSection>,
    selected: WorkspaceSection,
    enabled: Boolean,
    onSelect: (WorkspaceSection) -> Unit,
    onLogout: () -> Unit
) {
    Surface(Modifier.width(204.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("OmniAsset", Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            sections.forEach { section ->
                val active = section == selected
                Surface(
                    onClick = { onSelect(section) }, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(section.icon, contentDescription = null)
                        Text(section.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            TextButton(onClick = onLogout, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Выйти") }
        }
    }
}

@Composable
private fun WorkspaceBottomBar(
    sections: List<WorkspaceSection>,
    selected: WorkspaceSection,
    enabled: Boolean,
    onSelect: (WorkspaceSection) -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val primary = listOf(WorkspaceSection.EQUIPMENT, WorkspaceSection.REPORTS)
    val extra = sections.filterNot { it in primary }
    Box(Modifier.fillMaxWidth()) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            primary.forEach { section ->
                NavigationBarItem(
                    selected = section == selected,
                    onClick = { onSelect(section) }, enabled = enabled,
                    icon = { Icon(section.icon, contentDescription = null) },
                    label = { Text(section.title, maxLines = 1) }
                )
            }
            NavigationBarItem(
                selected = selected in extra,
                onClick = { expanded = true }, enabled = enabled,
                icon = { Icon(Lucide.Menu, contentDescription = null) },
                label = { Text("Ещё") }
            )
        }
        Box(Modifier.align(Alignment.TopEnd).padding(end = 8.dp)) {
            DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                extra.forEach { section ->
                    DropdownMenuItem(
                        text = { Text(section.title) },
                        leadingIcon = { Icon(section.icon, contentDescription = null) },
                        onClick = { expanded = false; onSelect(section) }
                    )
                }
                DropdownMenuItem(text = { Text("Выйти") }, onClick = { expanded = false; onLogout() })
            }
        }
    }
}

@Composable
internal expect fun WorkspaceBackHandler(onBack: () -> Unit)
