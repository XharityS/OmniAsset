package ru.echosystems.omni.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ru.echosystems.omni.app.navigation.AppNavigation

@Composable
fun OmniAssetApp() {
    MaterialTheme {
        AppNavigation()
    }
}
