package ru.echosystems.omni.app.feature.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberCsvFilePicker(onSelected: (CsvDestination?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        onSelected(uri?.let { destination ->
            CsvDestination { download ->
                withContext(Dispatchers.IO) {
                    val temporary = java.io.File.createTempFile("omniasset-", ".csv", context.cacheDir)
                    try {
                        temporary.outputStream().use { file ->
                            download { buffer, count -> file.write(buffer, 0, count) }
                        }
                        val output = checkNotNull(context.contentResolver.openOutputStream(destination, "wt"))
                        output.use { file -> temporary.inputStream().use { it.copyTo(file) } }
                    } finally {
                        temporary.delete()
                    }
                    Unit
                }
            }
        })
    }
    return { launcher.launch("omniasset-equipment.csv") }
}
