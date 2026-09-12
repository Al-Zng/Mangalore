package com.mangalore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.mangalore.app.localization.LocalizationManager
import com.mangalore.app.ui.nav.MangaloreApp
import com.mangalore.app.ui.theme.MangaloreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MangaloreTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LocalizationManager.language.layoutDirection) {
                    MangaloreApp()
                }
            }
        }
    }
}
