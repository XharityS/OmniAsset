package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.echosystems.omni.app.ui.OmniBackground
import ru.echosystems.omni.app.ui.OmniFilter

@Composable
fun EquipmentFormScreen(
    editing: Boolean,
    state: EquipmentFormState,
    onChange: (EquipmentFormRequest) -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
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
                Text(
                    if (editing) "Редактирование оборудования" else "Новое оборудование",
                    style = MaterialTheme.typography.headlineSmall
                )
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when {
                    state.loading -> CircularProgressIndicator()
                    !state.ready -> Button(onClick = onRetry) { Text("Повторить") }
                    else -> {
                        EquipmentFields(state, onChange)
                        Button(onClick = onSave, enabled = !state.saving) {
                            Text(if (state.saving) "Сохранение…" else "Сохранить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EquipmentFields(state: EquipmentFormState, onChange: (EquipmentFormRequest) -> Unit) {
    val draft = state.draft
    val enabled = !state.saving
    EquipmentField("Инвентарный номер *", draft.inventoryNumber, enabled) {
        onChange(draft.copy(inventoryNumber = it))
    }
    EquipmentField("Серийный номер", draft.serialNumber, enabled) {
        onChange(draft.copy(serialNumber = it))
    }
    EquipmentChoice("Модель *", draft.modelId, state.models, enabled, required = true) {
        onChange(draft.copy(modelId = it))
    }
    EquipmentChoice("Статус *", draft.statusId, state.statuses, enabled, required = true) {
        onChange(draft.copy(statusId = it))
    }
    EquipmentChoice("Расположение", draft.locationId, state.locations, enabled) {
        onChange(draft.copy(locationId = it))
    }
    EquipmentChoice("Подразделение", draft.departmentId, state.departments, enabled) {
        onChange(draft.copy(departmentId = it))
    }
    EquipmentField("Дата покупки (ГГГГ-ММ-ДД)", draft.purchaseDate, enabled) {
        onChange(draft.copy(purchaseDate = it))
    }
    EquipmentField("Дата ввода (ГГГГ-ММ-ДД)", draft.commissioningDate, enabled) {
        onChange(draft.copy(commissioningDate = it))
    }
    EquipmentField("Гарантия до (ГГГГ-ММ-ДД)", draft.warrantyUntil, enabled) {
        onChange(draft.copy(warrantyUntil = it))
    }
    EquipmentField("Стоимость", draft.purchasePrice, enabled) {
        onChange(draft.copy(purchasePrice = it))
    }
    EquipmentField("Примечание", draft.notes, enabled, singleLine = false) {
        onChange(draft.copy(notes = it))
    }
}

@Composable
internal fun EquipmentChoice(
    label: String,
    selectedId: Long?,
    options: List<ReferenceItem>,
    enabled: Boolean,
    required: Boolean = false,
    onSelect: (Long?) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        OmniFilter(
            label = if (options.isEmpty()) "Справочник пуст" else "Выбрать",
            selectedId = selectedId,
            options = options.map { it.id to it.name },
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
            clearLabel = if (required) null else "Не указано",
            enabled = enabled
        )
    }
}

@Composable
internal fun EquipmentField(
    label: String,
    value: String?,
    enabled: Boolean,
    singleLine: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value.orEmpty(),
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}
