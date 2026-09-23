package com.example.strong_vault.vault

enum class WeightUnit(val label: String) { LB("lb"), KG("kg") }

data class VaultSettings(
    val vaultPath: String = Defaults.VAULT_PATH,
    val weightUnit: WeightUnit = Defaults.WEIGHT_UNIT,
) {
    object Defaults {
        // Same shared-storage vault androidhomecal points at (its AGENDA_VAULT_PATH), so both
        // apps read/write the same daily notes.
        const val VAULT_PATH = "/storage/emulated/0/Documents/Vault"
        val WEIGHT_UNIT = WeightUnit.LB
    }
}
