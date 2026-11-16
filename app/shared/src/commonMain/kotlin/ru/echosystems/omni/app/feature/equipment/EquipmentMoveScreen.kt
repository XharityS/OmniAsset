package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.echosystems.omni.app.ui.OmniBackground

@Composable
fun EquipmentMoveScreen(
    state: EquipmentMoveState,
    onChange: (EquipmentMoveRequest) -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val draft = state.draft
    OmniBackground {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onBack, enabled = !state.saving) { Text("Назад") }
                Text("Перемещение оборудования", style = MaterialTheme.typography.headlineSmall)
                state.item?.let { Text("${it.inventoryNumber} · ${it.modelName}") }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when {
                    state.loading -> CircularProgressIndicator()
                    draft == null -> Button(onClick = onRetry) { Text("Повторить") }
                    else -> {
                        val enabled = !state.saving
                        EquipmentChoice("Расположение", draft.locationId, state.locations, enabled) {
                            onChange(draft.copy(locationId = it))
                        }
                        EquipmentChoice("Подразделение", draft.departmentId, state.departments, enabled) {
                            onChange(draft.copy(departmentId = it))
                        }
                        EquipmentChoice("Ответственный", draft.employeeId, state.employees, enabled) {
                            onChange(draft.copy(employeeId = it))
                        }
                        EquipmentField("Причина или комментарий", draft.comment, enabled, singleLine = false) {
                            onChange(draft.copy(comment = it))
                        }
                        Button(onClick = onSave, enabled = enabled && state.hasChanges) {
                            Text(if (state.saving) "Сохранение…" else "Сохранить")
                        }
                    }
                }
            }
        }
    }
}
