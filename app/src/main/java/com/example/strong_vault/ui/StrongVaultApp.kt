package com.example.strong_vault.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.strong_vault.AppContainer
import com.example.strong_vault.vault.StoragePermission
import com.example.strong_vault.ui.exercises.ExercisesScreen
import com.example.strong_vault.ui.history.HistoryScreen
import com.example.strong_vault.ui.permission.PermissionScreen
import com.example.strong_vault.ui.settings.SettingsScreen
import com.example.strong_vault.ui.stats.StatsScreen
import com.example.strong_vault.ui.today.TodayScreen

enum class AppTab(val label: String, val glyph: String) {
    TODAY("Today", "●"),
    HISTORY("History", "≡"),
    STATS("Stats", "≈"),
    EXERCISES("Exercises", "☰"),
    SETTINGS("Settings", "⚙"),
}

@Composable
fun StrongVaultApp(container: AppContainer) {
    var permissionGranted by remember { mutableStateOf(StoragePermission.isGranted()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = StoragePermission.isGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!permissionGranted) {
        val context = LocalContext.current
        PermissionScreen(onGrantClick = { context.startActivity(StoragePermission.requestIntent(context)) })
        return
    }

    var selectedTab by remember { mutableStateOf(AppTab.TODAY) }
    var settingsVersion by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.glyph) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        key(settingsVersion) {
            when (selectedTab) {
                AppTab.TODAY -> TodayScreen(
                    repository = container.repository,
                    settings = container.settings,
                    modifier = contentModifier,
                    selectedDate = selectedDate,
                    onSelectedDateChange = { selectedDate = it },
                )
                AppTab.HISTORY -> HistoryScreen(
                    repository = container.repository,
                    modifier = contentModifier,
                    onOpenDate = { date ->
                        selectedDate = date
                        selectedTab = AppTab.TODAY
                    },
                )
                AppTab.STATS -> StatsScreen(container.repository, contentModifier)
                AppTab.EXERCISES -> ExercisesScreen(container.repository, contentModifier)
                AppTab.SETTINGS -> SettingsScreen(
                    settings = container.settings,
                    modifier = contentModifier,
                    onSettingsChanged = { newSettings ->
                        container.updateSettings(newSettings)
                        settingsVersion++
                    },
                )
            }
        }
    }
}
