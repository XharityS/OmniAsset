package ru.echosystems.omni.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun WorkspaceBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
