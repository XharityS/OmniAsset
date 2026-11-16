package ru.echosystems.omni.app.feature.reports

import androidx.compose.runtime.Composable

internal fun interface CsvDestination {
    suspend fun save(download: suspend ((ByteArray, Int) -> Unit) -> Unit)
}

@Composable
internal expect fun rememberCsvFilePicker(onSelected: (CsvDestination?) -> Unit): () -> Unit
