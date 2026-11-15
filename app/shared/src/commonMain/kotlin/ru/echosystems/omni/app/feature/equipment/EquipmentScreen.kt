package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import ru.echosystems.omni.app.ui.OmniBackground
import ru.echosystems.omni.app.ui.OmniStatusChip

@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onStatus: (Long?) -> Unit,
    onCategory: (Long?) -> Unit,
    onLocation: (Long?) -> Unit,
    onItemClick: (Long) -> Unit
) {
    var searchFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { onRefresh() }

    OmniBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Оборудование", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("Учет и состояние компьютерной техники", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Icon(Lucide.RefreshCw, contentDescription = "Обновить")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Загружено", state.items.size.toString(), Modifier.weight(1f))
                SummaryCard("Выдано", state.items.count { it.statusCode == "assigned" }.toString(), Modifier.weight(1f))
                SummaryCard("Доступно", state.items.count { it.statusCode == "available" }.toString(), Modifier.weight(1f))
            }

            OutlinedTextField(
                value = state.search,
                onValueChange = { onSearchChanged(it); searchFocused = it.isNotEmpty() },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Lucide.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchFocused) IconButton(onClick = { onSearchChanged(""); searchFocused = false; onRefresh() }) {
                        Icon(Lucide.X, contentDescription = "Очистить")
                    }
                },
                label = { Text("Поиск по инвентарному номеру или модели") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ru.echosystems.omni.app.ui.OmniFilter("Категория", state.categoryId,
                    state.categories.map { it.id to it.name }, onCategory, Modifier.weight(1f))
                ru.echosystems.omni.app.ui.OmniFilter("Расположение", state.locationId,
                    state.locations.map { it.id to it.name }, onLocation, Modifier.weight(1f))
            }
            ru.echosystems.omni.app.ui.OmniFilter("Все статусы", state.statusIndex,
                equipmentStatuses.mapIndexed { index, status -> index.toLong() to status.second }, onStatus)
            state.filterError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.errorMessage?.let { message ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.items.isEmpty()) {
                EmptyEquipmentState(Modifier.fillMaxWidth().weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.items, key = { it.id }) { EquipmentCard(it, onItemClick) }
                    if (state.hasMore) item {
                        androidx.compose.material3.TextButton(onClick = onLoadMore, enabled = !state.isLoadingMore) {
                            Text(if (state.isLoadingMore) "Загрузка…" else "Загрузить ещё")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EquipmentCard(item: EquipmentRow, onItemClick: (Long) -> Unit) {
    Card(
        onClick = { onItemClick(item.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) { Box(contentAlignment = Alignment.Center) { Icon(Lucide.Package, null, tint = MaterialTheme.colorScheme.primary) } }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.inventoryNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${item.manufacturerName} ${item.modelName}")
                item.locationName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                item.responsibleName?.let { Text("Ответственный: $it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
            OmniStatusChip(item.statusCode, item.statusName)
        }
    }
}

@Composable
private fun EmptyEquipmentState(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Lucide.Package, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(12.dp))
        Text("Оборудование не найдено", style = MaterialTheme.typography.titleMedium)
        Text("Попробуйте изменить запрос или добавить первую запись", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
