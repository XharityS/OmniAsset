package ru.echosystems.omni.app.feature.reports

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.echosystems.omni.app.ui.OmniBackground
import ru.echosystems.omni.app.ui.OmniStatusChip

@Composable
internal fun ReportsScreen(
    state: ReportsState,
    filters: ReportFilters,
    onRefresh: () -> Unit,
    onExport: (CsvDestination?) -> Unit,
    onBack: () -> Unit
) {
    val pickFile = rememberCsvFilePicker(onExport)
    OmniBackground {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, enabled = !state.exporting) { Text("Назад") }
                    Text("Отчёты", style = MaterialTheme.typography.headlineSmall)
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Сводка по статусам", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Обновить") }
                        }
                        when {
                            state.loading -> CircularProgressIndicator()
                            state.loadingError != null -> Text(state.loadingError, color = MaterialTheme.colorScheme.error)
                            state.statuses.isEmpty() -> Text("Нет данных")
                            else -> {
                                Text("Всего в базе: ${state.total}")
                                state.statuses.forEach { status ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OmniStatusChip(status.statusCode, status.statusName)
                                        Text(status.amount.toString())
                                    }
                                }
                            }
                        }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Экспорт списка оборудования", style = MaterialTheme.typography.titleMedium)
                        if (filters.labels.isEmpty()) Text("Всё оборудование")
                        else filters.labels.forEach { Text(it) }
                        Button(onClick = pickFile, enabled = !state.exporting) {
                            Text(if (state.exporting) "Сохранение…" else "Сохранить CSV")
                        }
                        if (state.exported) Text("Файл сохранён")
                        state.exportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
