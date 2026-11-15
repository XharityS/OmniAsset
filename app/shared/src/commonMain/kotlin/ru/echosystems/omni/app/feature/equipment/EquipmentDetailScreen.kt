package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Wrench
import ru.echosystems.omni.app.ui.OmniBackground
import ru.echosystems.omni.app.ui.OmniStatusChip

@Composable
fun EquipmentDetailScreen(
    equipmentId: Long,
    state: EquipmentDetailUiState,
    onLoad: (Long) -> Unit,
    onBack: () -> Unit,
    onChangeStatus: (String) -> Unit,
    onEdit: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null
) {
    LaunchedEffect(equipmentId) { onLoad(equipmentId) }
    OmniBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Lucide.ArrowLeft, "Назад") }
                Text("Карточка оборудования", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            if (state.isLoading && state.item == null) {
                Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            } else {
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.item?.let { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Package, null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.inventoryNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${item.manufacturerName} ${item.modelName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OmniStatusChip(item.statusCode, item.statusName)
                            }
                            DetailLine("Серийный номер", item.serialNumber)
                            DetailLine("Категория", item.categoryName)
                            DetailLine("Расположение", item.locationName)
                            DetailLine("Подразделение", item.departmentName)
                            DetailLine("Ответственный", item.responsibleName)
                            DetailLine("Дата покупки", item.purchaseDate)
                            DetailLine("Гарантия до", item.warrantyUntil)
                            DetailLine("Стоимость", item.purchasePrice?.let { "$it ₽" })
                            item.notes?.let { DetailLine("Примечание", it) }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            onEdit?.let { androidx.compose.material3.TextButton(onClick = it, enabled = !state.isActionLoading && state.item?.id == equipmentId) { Text("Редактировать") } }
                            if (onMove != null && item.statusCode != "written_off") {
                                androidx.compose.material3.TextButton(
                                    onClick = onMove,
                                    enabled = !state.isLoading && !state.isActionLoading
                                ) { Text("Переместить или сменить ответственного") }
                            }
                            Text("Быстрые действия", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onChangeStatus("available") }, enabled = !state.isActionLoading) { Text("Доступно") }
                                Button(onClick = { onChangeStatus("repair") }, enabled = !state.isActionLoading) { Icon(Lucide.Wrench, null); Spacer(Modifier.size(6.dp)); Text("В ремонт") }
                            }
                            state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                Text("История", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (state.history.isEmpty()) Text("История пока пуста", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.history) { event ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))) {
                            Column(Modifier.padding(14.dp)) { Text(event.type); Text(event.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); event.comment?.takeIf { it.isNotBlank() }?.let { Text(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(it, fontWeight = FontWeight.Medium)
        }
    }
}
