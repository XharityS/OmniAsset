package ru.echosystems.omni

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.echosystems.omni.app.OmniAssetApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OmniAsset",
    ) {
        OmniAssetApp()
    }
}