package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.local.SettingsDataStore
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.KernelManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = SettingsDataStore(this)

        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = "dark")
            val dynamicColor by settings.dynamicColor.collectAsState(initial = true)

            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            KernelManagerTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
                AppNavGraph()
            }
        }
    }
}
