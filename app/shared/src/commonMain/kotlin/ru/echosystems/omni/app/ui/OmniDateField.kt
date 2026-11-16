package ru.echosystems.omni.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OmniDateField(
    label: String,
    value: String?,
    enabled: Boolean,
    error: String?,
    onChange: (String) -> Unit
) {
    var showCalendar by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value.orEmpty().let { if ('-' in it) FormDate.parse(it)?.toDisplay() ?: it else it },
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("ДД.ММ.ГГГГ") },
        supportingText = { Text(error ?: "ДД.ММ.ГГГГ или ГГГГ-ММ-ДД") },
        isError = error != null,
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showCalendar = true }, enabled = enabled) {
                Icon(Lucide.Calendar, contentDescription = "Выбрать дату")
            }
        }
    )
    if (showCalendar) {
        val date = FormDate.parse(value.orEmpty())
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date?.toUtcMillis(),
            yearRange = minOf(1900, date?.year ?: 1900)..maxOf(2100, date?.year ?: 2100)
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { onChange(FormDate.fromUtcMillis(it).toDisplay()) }
                        showCalendar = false
                    },
                    enabled = picker.selectedDateMillis != null
                ) { Text("Выбрать") }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Отмена") } }
        ) {
            DatePicker(state = picker, showModeToggle = false)
        }
    }
}

@Composable
internal fun RequiredFieldsNote() {
    Text("* — обязательное поле", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
}
