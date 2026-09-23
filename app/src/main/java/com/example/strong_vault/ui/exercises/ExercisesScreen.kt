package com.example.strong_vault.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.strong_vault.workout.WorkoutRepository
import com.example.strong_vault.workout.model.WorkoutConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ExercisesScreen(repository: WorkoutRepository, modifier: Modifier = Modifier) {
    var config by remember { mutableStateOf<WorkoutConfig?>(null) }
    var canCreateDefault by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        config = withContext(Dispatchers.IO) { repository.loadConfig() }
        canCreateDefault = withContext(Dispatchers.IO) { repository.canCreateDefaultConfig() }
    }

    LaunchedEffect(repository) { reload() }

    val current = config ?: return

    Column(modifier.fillMaxSize()) {
        if (canCreateDefault) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "No Workout.md found in your vault - showing built-in defaults. " +
                        "Create one to customize your exercise list and templates by hand.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { repository.writeDefaultConfig() }
                            reload()
                        }
                    },
                ) { Text("Create Workout.md with defaults") }
            }
            HorizontalDivider()
        }

        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                Text(
                    "Templates",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(current.templates.entries.toList()) { (name, exercises) ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text(exercises.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item {
                Text(
                    "All exercises",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(current.exercises) { exercise ->
                Text(
                    exercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}
