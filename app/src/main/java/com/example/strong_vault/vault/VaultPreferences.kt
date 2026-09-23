package com.example.strong_vault.vault

import android.content.Context

class VaultPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): VaultSettings = VaultSettings(
        vaultPath = prefs.getString(KEY_VAULT_PATH, VaultSettings.Defaults.VAULT_PATH)
            ?: VaultSettings.Defaults.VAULT_PATH,
        weightUnit = prefs.getString(KEY_WEIGHT_UNIT, null)
            ?.let { name -> WeightUnit.entries.find { it.name == name } }
            ?: VaultSettings.Defaults.WEIGHT_UNIT,
    )

    fun save(settings: VaultSettings) {
        prefs.edit()
            .putString(KEY_VAULT_PATH, settings.vaultPath)
            .putString(KEY_WEIGHT_UNIT, settings.weightUnit.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "strong_vault_prefs"
        private const val KEY_VAULT_PATH = "vaultPath"
        private const val KEY_WEIGHT_UNIT = "weightUnit"
    }
}
