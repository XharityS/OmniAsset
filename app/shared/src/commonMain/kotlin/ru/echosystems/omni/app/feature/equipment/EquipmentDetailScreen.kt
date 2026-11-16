package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import ru.echosystems.omni.app.ui.OmniStatusChip

@Composable
fun EquipmentDetailScreen(
    equipmentId: Long,
    state: EquipmentDetailUiState,
    onLoad: (Long) -> Unit,
    onBack: () -> Unit,
    onChangeStatus: ((String) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null
) {
    LaunchedEffect(equipmentId) { onLoad(equipmentId) }
    val equipment = state.item
    val busy = state.isLoading || state.isActionLoading
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, enabled = !state.isActionLoading) { Icon(Lucide.ArrowLeft, "Назад") }
                    Text("Техника", style = MaterialTheme.typography.titleLarge)
                }
            }
            LazyColumn(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.errorMessage != null) item {
                    Text(state.errorMessage, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onLoad(equipmentId) }, enabled = !busy) { Text("Повторить") }
                }
                if (equipment == null && state.errorMessage == null) item {
                    CircularProgressIndicator(Modifier.padding(20.dp))
                }
                if (equipment != null) {
                    item {
                        Column(Modifier.padding(top = 18.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${equipment.manufacturerName} ${equipment.modelName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Инв. номер: ${equipment.inventoryNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Статус", Modifier.weight(1f))
                                    OmniStatusChip(equipment.statusCode, equipment.statusName)
                                }
                                HorizontalDivider()
                                DetailLine("Место", equipment.locationName ?: "—")
                                HorizontalDivider()
                                DetailLine("Ответственный", equipment.responsibleName ?: "—")
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (onMove != null && equipment.statusCode != "written_off") {
                                Button(onClick = onMove, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                    Text("Переместить")
                                }
                            }
                            if (onChangeStatus != null) StatusAction(equipment, busy, onChangeStatus)
                            if (onEdit != null) TextButton(onClick = onEdit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                Text("Редактировать карточку")
                            }
                            state.actionMessage?.let { Text(it) }
                        }
                    }
                    item {
                        Text("История", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge)
                    }
                    if (state.history.isEmpty()) item {
                        Text("История пока пуста", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.history) { event ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(when (event.type) {
                                    "movement" -> "Перемещение или смена ответственного"
                                    "status_change" -> "Изменение статуса"
                                    else -> event.type
                                })
                                Text(event.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                event.comment?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            }
                        }
                    }
                    item {
                        Text("Сведения", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge)
                        Card(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                            Column(Modifier.padding(horizontal = 14.dp)) {
                                DetailLine("Категория", equipment.categoryName)
                                DetailLine("Серийный номер", equipment.serialNumber)
                                DetailLine("Подразделение", equipment.departmentName)
                                DetailLine("Дата покупки", equipment.purchaseDate)
                                DetailLine("Дата ввода", equipment.commissioningDate)
                                DetailLine("Гарантия до", equipment.warrantyUntil)
                                DetailLine("Стоимость", equipment.purchasePrice?.let { "$it ₽" })
                                DetailLine("Примечание", equipment.notes)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusAction(item: EquipmentDetails, busy: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Изменить статус")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            equipmentStatuses.filter { (code, _) ->
                code != item.statusCode && code != "written_off" && (code != "assigned" || item.responsibleName != null)
            }.forEach { (code, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onChange(code) })
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}
