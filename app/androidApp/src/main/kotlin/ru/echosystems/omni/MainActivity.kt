package ru.echosystems.omni

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ru.echosystems.omni.app.OmniAssetApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OmniAssetApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    OmniAssetApp()
}