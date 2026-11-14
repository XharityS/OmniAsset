package ru.echosystems.omni.app.feature.equipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Search

@Composable
fun EquipmentScreen(state: EquipmentUiState, onSearchChanged: (String) -> Unit, onRefresh: () -> Unit) {
    LaunchedEffect(Unit) { onRefresh() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Оборудование", style = MaterialTheme.typography.headlineSmall); Text("Всего: ${state.items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onRefresh) { Icon(Lucide.RefreshCw, "Обновить") }
        }
        OutlinedTextField(value = state.search, onValueChange = onSearchChanged, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Lucide.Search, null) }, label = { Text("Поиск по инвентарному номеру или модели") }, singleLine = true)
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) CircularProgressIndicator()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(item.inventoryNumber, style = MaterialTheme.typography.titleMedium); Text("${item.manufacturerName} ${item.modelName}"); Text(item.statusName, color = MaterialTheme.colorScheme.primary); item.locationName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            }
        }
    }
}
