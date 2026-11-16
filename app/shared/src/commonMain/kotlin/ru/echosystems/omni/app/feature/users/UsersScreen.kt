package ru.echosystems.omni.app.feature.users

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.echosystems.omni.app.feature.equipment.EquipmentField
import ru.echosystems.omni.app.ui.OmniFilter
import ru.echosystems.omni.app.ui.RequiredFieldsNote

@Composable
internal fun UsersScreen(
    state: UsersState,
    currentUserId: String,
    onEdit: (ManagedUser?) -> Unit,
    onChange: (UserDraft?) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val draft = state.draft
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, enabled = !state.saving) {
                    Text("Назад")
                }
                Text("Пользователи", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                if (!state.loading && draft == null && state.error == null) {
                    TextButton(onClick = { onEdit(null) }) { Text("Добавить") }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                state.loading -> CircularProgressIndicator()
                draft != null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RequiredFieldsNote()
                    UserFields(draft, state.saving, state.validationAttempted, currentUserId, onChange)
                    Button(onClick = onSave, enabled = !state.saving) {
                        Text(if (state.saving) "Сохранение…" else "Сохранить")
                    }
                }
                state.error != null -> Button(onClick = onRetry) { Text("Повторить") }
                state.users.isEmpty() -> Text("Пользователей пока нет")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.users, key = { it.id }) { user ->
                        Card(onClick = { onEdit(user) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                                Text("${user.username} · ${UserRole.label(user.role)}")
                                if (!user.isActive) Text("Заблокирован", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserFields(draft: UserDraft, saving: Boolean, showErrors: Boolean, currentUserId: String, onChange: (UserDraft) -> Unit) {
    EquipmentField("Логин", draft.username, !saving && draft.id == null, required = draft.id == null,
        error = "Укажите логин".takeIf { showErrors && draft.id == null && draft.username.isBlank() }) { onChange(draft.copy(username = it)) }
    EquipmentField("ФИО", draft.fullName, !saving, required = true,
        error = "Укажите ФИО".takeIf { showErrors && draft.fullName.isBlank() }) { onChange(draft.copy(fullName = it)) }
    EquipmentField("Электронная почта", draft.email, !saving) { onChange(draft.copy(email = it)) }
    if (draft.id == null) {
        OutlinedTextField(
            draft.password, { onChange(draft.copy(password = it)) },
            label = { Text("Пароль *") },
            isError = showErrors && (draft.password.length < 8 || draft.password.encodeToByteArray().size > 72),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth()
        )
    }
    if (draft.id == currentUserId) {
        Text("Роль: ${UserRole.label(draft.role)}")
    } else {
        OmniFilter(
            label = "Роль *",
            selectedId = UserRole.entries.firstOrNull { it.code == draft.role }?.ordinal?.toLong(),
            options = UserRole.entries.map { it.ordinal.toLong() to it.label },
            onSelect = { id -> id?.let { onChange(draft.copy(role = UserRole.entries[it.toInt()].code)) } },
            clearLabel = null,
            enabled = !saving
        )
        if (draft.id != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(draft.active, { onChange(draft.copy(active = it)) }, enabled = !saving)
                Text("Разрешить вход")
            }
        }
    }
}
