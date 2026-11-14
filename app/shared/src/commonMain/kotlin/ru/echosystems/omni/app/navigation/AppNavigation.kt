package ru.echosystems.omni.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.echosystems.omni.app.feature.auth.AuthScreen

@Composable
fun AppNavigation() {
    Surface(modifier = Modifier.fillMaxSize()) {
        AuthScreen()
    }
}
