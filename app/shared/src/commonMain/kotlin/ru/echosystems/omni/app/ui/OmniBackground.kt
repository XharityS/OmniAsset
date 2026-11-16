package ru.echosystems.omni.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import omniasset.app.shared.generated.resources.Res
import omniasset.app.shared.generated.resources.omni_wallpaper_dark
import omniasset.app.shared.generated.resources.omni_wallpaper_light

@Composable
fun OmniBackground(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(if (dark) Res.drawable.omni_wallpaper_dark else Res.drawable.omni_wallpaper_light),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (dark) 0.34f else 0.18f)
        )
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)))
        content()
    }
}
