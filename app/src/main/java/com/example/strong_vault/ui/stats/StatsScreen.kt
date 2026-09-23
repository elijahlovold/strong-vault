package com.example.strong_vault.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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
import com.example.strong_vault.stats.WorkoutStats
import com.example.strong_vault.ui.components.HeatmapCalendar
import com.example.strong_vault.ui.components.SimpleLineChart
import com.example.strong_vault.workout.WorkoutRepository
import com.example.strong_vault.workout.model.WorkoutSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val SCAN_WINDOW_DAYS = 365
private const val HEATMAP_WEEKS = 20

@Composable
fun StatsScreen(repository: WorkoutRepository, modifier: Modifier = Modifier) {
    var sections by remember { mutableStateOf<List<Pair<LocalDate, WorkoutSection>>?>(null) }
    var selectedExercise by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        sections = withContext(Dispatchers.IO) {
            repository.recentSections(LocalDate.now(), SCAN_WINDOW_DAYS)
        }
    }

    val loaded = sections ?: run {
        Text("Loading...", modifier = modifier.padding(16.dp))
        return
    }

    val exerciseNames = remember(loaded) { WorkoutStats.distinctExerciseNames(loaded) }
    if (selectedExercise == null) selectedExercise = exerciseNames.firstOrNull()
    val dailyCounts = remember(loaded) { WorkoutStats.dailySetCounts(loaded) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Frequency (last $HEATMAP_WEEKS weeks)", style = MaterialTheme.typography.titleMedium)
        HeatmapCalendar(dailyCounts, HEATMAP_WEEKS, Modifier.padding(top = 8.dp, bottom = 24.dp))

        Text("Estimated 1RM progression", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            exerciseNames.forEach { name ->
                AssistChip(
                    onClick = { selectedExercise = name },
                    label = { Text(name) },
                )
            }
        }

        val exercise = selectedExercise
        if (exercise == null) {
            Text(
                "No logged sets with an absolute weight yet.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val history = remember(loaded, exercise) { WorkoutStats.exerciseHistory(loaded, exercise) }
            if (history.isEmpty()) {
                Text("No data for $exercise yet.", modifier = Modifier.padding(top = 8.dp))
            } else {
                SimpleLineChart(history.map { it.estimated1Rm.toFloat() })
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                val latest = history.last()
                Text(
                    "Latest: ${"%.0f".format(latest.estimated1Rm)} est. 1RM, top set ${"%.0f".format(latest.topWeight)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val best = history.maxBy { it.estimated1Rm }
                Text(
                    "Best: ${"%.0f".format(best.estimated1Rm)} est. 1RM on ${best.date}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
