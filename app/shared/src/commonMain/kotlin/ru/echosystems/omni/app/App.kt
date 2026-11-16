package ru.echosystems.omni.app

import androidx.compose.runtime.Composable
import ru.echosystems.omni.app.navigation.AppNavigation
import ru.echosystems.omni.app.theme.OmniTheme

@Composable
fun OmniAssetApp() {
    OmniTheme {
        AppNavigation()
    }
}
