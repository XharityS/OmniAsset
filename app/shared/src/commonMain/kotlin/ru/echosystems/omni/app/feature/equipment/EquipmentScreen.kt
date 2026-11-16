package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.ChartNoAxesCombined
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.User
import com.composables.icons.lucide.X
import androidx.compose.ui.graphics.vector.ImageVector
import ru.echosystems.omni.app.ui.OmniFilter
import ru.echosystems.omni.app.ui.OmniStatusChip

private data class Section(val name: String, val icon: ImageVector? = null, val open: (() -> Unit)? = null)

@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onStatus: (Long?) -> Unit,
    onCategory: (Long?) -> Unit,
    onLocation: (Long?) -> Unit,
    onAdd: (() -> Unit)? = null,
    onDirectory: (() -> Unit)? = null,
    onEmployees: (() -> Unit)? = null,
    onPlaces: (() -> Unit)? = null,
    onUsers: (() -> Unit)? = null,
    onReports: () -> Unit,
    onLogout: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }
    val sections = buildList {
        add(Section("Техника", Lucide.Package))
        onEmployees?.let { add(Section("Сотрудники", Lucide.User, it)) }
        onPlaces?.let { add(Section("Места", Lucide.MapPin, it)) }
        onDirectory?.let { add(Section("Справочники", Lucide.Package, it)) }
        add(Section("Отчёты", Lucide.ChartNoAxesCombined, onReports))
        onUsers?.let { add(Section("Пользователи", Lucide.User, it)) }
        add(Section("Выйти", open = onLogout))
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 900.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) Sidebar(sections)
            Column(Modifier.weight(1f)) {
                EquipmentTopBar(sections, wide, state.isLoading, onRefresh)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = if (wide) 24.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (wide) 0.dp else 8.dp)
                ) {
                    item {
                        Column(Modifier.padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Техника", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EquipmentSearch(state.search, onSearchChanged, Modifier.weight(1f))
                                if (wide && onAdd != null) AddEquipmentButton(onAdd)
                            }
                            if (!wide && onAdd != null) AddEquipmentButton(onAdd)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OmniFilter("Категория", state.categoryId, state.categories.map { it.id to it.name }, onCategory, Modifier.weight(1f))
                                OmniFilter("Место", state.locationId, state.locations.map { it.id to it.name }, onLocation, Modifier.weight(1f))
                                OmniFilter("Статус", state.statusIndex, equipmentStatuses.mapIndexed { index, status -> index.toLong() to status.second }, onStatus, Modifier.weight(1f))
                            }
                            state.filterError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    when {
                        state.isLoading -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        state.items.isEmpty() && state.errorMessage == null -> item {
                            Text("Оборудование не найдено", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            if (wide && state.items.isNotEmpty()) item { EquipmentTableHeader() }
                            items(state.items, key = { it.id }) { equipment ->
                                if (wide) EquipmentTableRow(equipment) { onItemClick(equipment.id) }
                                else EquipmentListCard(equipment) { onItemClick(equipment.id) }
                            }
                        }
                    }
                    if (state.hasMore && !state.isLoading) item {
                        FilledTonalButton(onClick = onLoadMore, enabled = !state.isLoadingMore, modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(if (state.isLoadingMore) "Загрузка…" else "Загрузить ещё")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(sections: List<Section>) {
    Surface(Modifier.width(204.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("OmniAsset", Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            for (section in sections) {
                val selected = section.open == null
                Surface(
                    modifier = Modifier.fillMaxWidth().then(
                        section.open?.let { Modifier.clickable(onClick = it) } ?: Modifier
                    ),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        section.icon?.let { Icon(it, contentDescription = null) }
                        Text(section.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentTopBar(sections: List<Section>, wide: Boolean, loading: Boolean, onRefresh: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!wide) Box {
                IconButton(onClick = { showMenu = true }) { Icon(Lucide.Menu, "Разделы") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    sections.forEach { section ->
                        val open = section.open
                        DropdownMenuItem(text = { Text(section.name) }, onClick = { showMenu = false; open?.invoke() })
                    }
                }
            }
            Text("Техника", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onRefresh, enabled = !loading) { Icon(Lucide.RefreshCw, "Обновить") }
        }
    }
}

@Composable
private fun EquipmentSearch(search: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = search, onValueChange = onChange, modifier = modifier, singleLine = true,
        shape = MaterialTheme.shapes.medium,
        leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
        trailingIcon = {
            if (search.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Lucide.X, "Очистить") }
        },
        placeholder = { Text("Инвентарный номер, модель или бренд", maxLines = 1, overflow = TextOverflow.Ellipsis) }
    )
}

@Composable
private fun AddEquipmentButton(onAdd: () -> Unit) {
    Button(onClick = onAdd) {
        Icon(Lucide.Plus, contentDescription = null)
        Text("Добавить технику", Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun EquipmentTableHeader() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TableText("Инв. номер", 0.9f, heading = true)
            TableText("Тип и модель", 1.5f, heading = true)
            TableText("Статус", 1.2f, heading = true)
            TableText("Место", 1f, heading = true)
            TableText("Ответственный", 1.2f, heading = true)
        }
    }
}

@Composable
private fun EquipmentTableRow(item: EquipmentRow, onOpen: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                TableText(item.inventoryNumber, 0.9f)
                Column(Modifier.weight(1.5f)) {
                    Text("${item.manufacturerName} ${item.modelName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.weight(1.2f)) { OmniStatusChip(item.statusCode, item.statusName) }
                TableText(item.locationName ?: "—", 1f)
                TableText(item.responsibleName ?: "—", 1.2f)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RowScope.TableText(value: String, width: Float, heading: Boolean = false) {
    Text(
        value, Modifier.weight(width), maxLines = 1, overflow = TextOverflow.Ellipsis,
        style = if (heading) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun EquipmentListCard(item: EquipmentRow, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${item.manufacturerName} ${item.modelName}", style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(item.inventoryNumber, item.locationName).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            OmniStatusChip(item.statusCode, item.statusName)
        }
    }
}
