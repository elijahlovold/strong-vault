package com.example.strong_vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.strong_vault.ui.StrongVaultApp
import com.example.strong_vault.ui.theme.StrongVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer(applicationContext)
        setContent {
            StrongVaultTheme {
                StrongVaultApp(container)
            }
        }
    }
}