package com.example.strong_vault

import android.content.Context
import com.example.strong_vault.vault.VaultPreferences
import com.example.strong_vault.vault.VaultSettings
import com.example.strong_vault.workout.WorkoutRepository
import java.io.File

/** Minimal manual DI: a repository bound to whatever vault path is currently configured. Recreated
 *  only when the path setting itself changes, so normal navigation doesn't re-touch the filesystem. */
class AppContainer(context: Context) {
    val preferences = VaultPreferences(context)

    var settings: VaultSettings = preferences.load()
        private set

    var repository: WorkoutRepository = WorkoutRepository(File(settings.vaultPath))
        private set

    fun updateSettings(newSettings: VaultSettings) {
        settings = newSettings
        preferences.save(newSettings)
        repository = WorkoutRepository(File(newSettings.vaultPath))
    }
}
