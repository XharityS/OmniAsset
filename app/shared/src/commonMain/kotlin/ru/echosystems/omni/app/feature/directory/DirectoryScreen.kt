package ru.echosystems.omni.app.feature.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.echosystems.omni.app.feature.equipment.EquipmentChoice
import ru.echosystems.omni.app.feature.equipment.EquipmentField
import ru.echosystems.omni.app.feature.equipment.ReferenceItem
import ru.echosystems.omni.app.ui.OmniBackground

@Composable
internal fun DirectoryScreen(
    state: DirectoryState,
    onOpen: (DirectoryType) -> Unit,
    onSelect: (DirectoryEntry) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val type = state.type
    val draft = state.draft
    OmniBackground {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.widthIn(max = 680.dp).fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, enabled = !state.saving) { Text("Назад") }
                    Text(type?.title ?: "Справочники", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    if (type != null && draft == null && !state.loading && state.error == null) {
                        TextButton(onClick = { onSelect(DirectoryEntry()) }) { Text("Добавить") }
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when {
                    type == null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DirectoryType.entries) { directory ->
                            Card(onClick = { onOpen(directory) }, modifier = Modifier.fillMaxWidth()) {
                                Text(directory.title, Modifier.padding(16.dp))
                            }
                        }
                    }
                    state.loading -> CircularProgressIndicator()
                    draft != null -> Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DirectoryFields(type, state, draft, onSelect)
                        Button(onClick = onSave, enabled = !state.saving) {
                            Text(if (state.saving) "Сохранение…" else "Сохранить")
                        }
                    }
                    state.error != null -> Button(onClick = { onOpen(type) }) { Text("Повторить") }
                    state.entries.isEmpty() -> Text("Записей пока нет")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.entries, key = { requireNotNull(it.id) }) { entry ->
                            Card(onClick = { onSelect(entry) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                                    val subtitle = when (type) {
                                        DirectoryType.MODELS -> state.manufacturers.find { it.id == entry.manufacturerId }?.name
                                        DirectoryType.LOCATIONS -> listOfNotNull(entry.building, entry.roomNumber).joinToString(" · ")
                                        DirectoryType.EMPLOYEES -> if (!entry.isActive) "Неактивен" else entry.position
                                        else -> entry.description
                                    }
                                    subtitle?.takeIf { it.isNotBlank() }?.let { Text(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryFields(
    type: DirectoryType,
    state: DirectoryState,
    draft: DirectoryEntry,
    onChange: (DirectoryEntry) -> Unit
) {
    val enabled = !state.saving
    Text(if (draft.id == null) "Новая запись" else "Редактирование", style = MaterialTheme.typography.titleMedium)
    EquipmentField(if (type == DirectoryType.EMPLOYEES) "ФИО *" else "Название *", draft.name, enabled) {
        onChange(draft.copy(name = it))
    }
    if (type in setOf(DirectoryType.CATEGORIES, DirectoryType.DEPARTMENTS, DirectoryType.LOCATIONS, DirectoryType.MODELS)) {
        EquipmentField("Описание", draft.description, enabled, singleLine = false) {
            onChange(draft.copy(description = it))
        }
    }
    when (type) {
        DirectoryType.LOCATIONS -> {
            EquipmentField("Корпус", draft.building, enabled) { onChange(draft.copy(building = it)) }
            EquipmentField("Номер кабинета", draft.roomNumber, enabled) { onChange(draft.copy(roomNumber = it)) }
        }
        DirectoryType.MODELS -> {
            EquipmentChoice("Категория *", draft.categoryId, state.categories.asChoices(), enabled, required = true) {
                onChange(draft.copy(categoryId = it))
            }
            EquipmentChoice("Производитель *", draft.manufacturerId, state.manufacturers.asChoices(), enabled, required = true) {
                onChange(draft.copy(manufacturerId = it))
            }
            if (state.categories.isEmpty() || state.manufacturers.isEmpty()) {
                Text("Сначала добавьте категорию и производителя")
            }
        }
        DirectoryType.EMPLOYEES -> {
            EquipmentField("Должность", draft.position, enabled) { onChange(draft.copy(position = it)) }
            EquipmentField("Подразделение", draft.department, enabled) { onChange(draft.copy(department = it)) }
            EquipmentField("Телефон", draft.phone, enabled) { onChange(draft.copy(phone = it)) }
            EquipmentField("Электронная почта", draft.email, enabled) { onChange(draft.copy(email = it)) }
        }
        else -> Unit
    }
}

private fun List<DirectoryEntry>.asChoices() = mapNotNull { entry ->
    entry.id?.let { ReferenceItem(it, entry.name) }
}
