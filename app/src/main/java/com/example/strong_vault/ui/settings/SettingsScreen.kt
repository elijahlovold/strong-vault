package com.example.strong_vault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.strong_vault.vault.VaultSettings
import com.example.strong_vault.vault.WeightUnit

@Composable
fun SettingsScreen(
    settings: VaultSettings,
    modifier: Modifier = Modifier,
    onSettingsChanged: (VaultSettings) -> Unit,
) {
    var vaultPath by remember(settings) { mutableStateOf(settings.vaultPath) }
    var weightUnit by remember(settings) { mutableStateOf(settings.weightUnit) }
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Vault path", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = vaultPath,
            onValueChange = { vaultPath = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text("Weight unit", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightUnit.entries.forEach { unit ->
                FilterChip(
                    selected = weightUnit == unit,
                    onClick = { weightUnit = unit },
                    label = { Text(unit.label) },
                )
            }
        }
        Text(
            "The unit only labels how numbers are displayed - the note format itself doesn't record " +
                "a unit, so keep it consistent once you start logging.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )

        Button(onClick = { onSettingsChanged(VaultSettings(vaultPath, weightUnit)) }) {
            Text("Save")
        }

        Text(
            "Version $versionName",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}
