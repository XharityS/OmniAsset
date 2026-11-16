package ru.echosystems.omni.app.feature.reports

import androidx.compose.runtime.Composable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberCsvFilePicker(onSelected: (CsvDestination?) -> Unit): () -> Unit = {
    val chooser = JFileChooser().apply { selectedFile = File("omniasset-equipment.csv") }
    val destination = if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile.absoluteFile
        val confirmed = !file.exists() || JOptionPane.showConfirmDialog(
            chooser, "Заменить файл ${file.name}?", "Сохранить CSV", JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION
        if (confirmed) CsvDestination { download ->
            withContext(Dispatchers.IO) {
                val temporary = Files.createTempFile(file.parentFile.toPath(), ".omniasset-", ".csv")
                try {
                    Files.newOutputStream(temporary).use { output ->
                        download { buffer, count -> output.write(buffer, 0, count) }
                    }
                    Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } finally {
                    Files.deleteIfExists(temporary)
                }
                Unit
            }
        } else null
    } else null
    onSelected(destination)
}
