package com.example.strong_vault.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.strong_vault.workout.WorkoutDaySummary
import com.example.strong_vault.workout.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SCAN_WINDOW_DAYS = 180
private val LABEL_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")

@Composable
fun HistoryScreen(
    repository: WorkoutRepository,
    modifier: Modifier = Modifier,
    onOpenDate: (LocalDate) -> Unit,
) {
    var summaries by remember { mutableStateOf<List<WorkoutDaySummary>?>(null) }

    LaunchedEffect(repository) {
        summaries = withContext(Dispatchers.IO) {
            repository.recentSummaries(LocalDate.now(), SCAN_WINDOW_DAYS)
        }
    }

    val list = summaries
    if (list == null) {
        Box(modifier.fillMaxSize()) {
            Text("Loading...", modifier = Modifier.padding(16.dp))
        }
        return
    }
    if (list.isEmpty()) {
        Box(modifier.fillMaxSize()) {
            Text(
                "No workouts logged in the last $SCAN_WINDOW_DAYS days.",
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(list, key = { it.date }) { summary ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDate(summary.date) }
                    .padding(16.dp),
            ) {
                Text(
                    text = summary.date.format(LABEL_FORMAT) + (summary.day?.let { " — $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${summary.exerciseCount} exercises, ${summary.setCount} sets",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider()
        }
    }
}
