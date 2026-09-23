package com.example.strong_vault.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.strong_vault.vault.VaultSettings
import com.example.strong_vault.workout.SaveOutcome
import com.example.strong_vault.workout.WorkoutDayState
import com.example.strong_vault.workout.WorkoutRepository
import com.example.strong_vault.workout.WorkoutSectionParser
import com.example.strong_vault.workout.WorkoutSectionResult
import com.example.strong_vault.workout.WorkoutSectionWriter
import com.example.strong_vault.workout.model.WorkoutConfig
import com.example.strong_vault.workout.model.WorkoutExercise
import com.example.strong_vault.workout.model.WorkoutSection
import com.example.strong_vault.workout.model.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private sealed class TodayUi {
    object Loading : TodayUi()
    data class Editable(val base: WorkoutDayState, val section: WorkoutSection) : TodayUi()
    data class Malformed(val reason: String) : TodayUi()
}

private val DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
fun TodayScreen(
    repository: WorkoutRepository,
    settings: VaultSettings,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit,
) {
    var ui by remember { mutableStateOf<TodayUi>(TodayUi.Loading) }
    var config by remember { mutableStateOf(WorkoutConfig.DEFAULT) }
    var banner by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        ui = TodayUi.Loading
        val date = selectedDate
        val (day, cfg) = withContext(Dispatchers.IO) {
            repository.loadDay(date) to repository.loadConfig()
        }
        config = cfg
        ui = when (val r = day.result) {
            is WorkoutSectionResult.Malformed -> TodayUi.Malformed(r.reason)
            else -> TodayUi.Editable(day, repository.baseSectionOf(r))
        }
    }

    LaunchedEffect(selectedDate, repository) { reload() }

    Column(modifier = modifier.fillMaxSize()) {
        DateNavRow(
            date = selectedDate,
            onPrev = { onSelectedDateChange(selectedDate.minusDays(1)) },
            onNext = { onSelectedDateChange(selectedDate.plusDays(1)) },
            onToday = { onSelectedDateChange(LocalDate.now()) },
        )
        HorizontalDivider()
        banner?.let {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(it, modifier = Modifier.padding(12.dp))
            }
        }

        when (val state = ui) {
            TodayUi.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            is TodayUi.Malformed -> MalformedView(state.reason)

            is TodayUi.Editable -> EditableWorkout(
                section = state.section,
                config = config,
                onSectionChange = { newSection -> ui = state.copy(section = newSection) },
                onSave = { finalSection ->
                    scope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            repository.save(selectedDate, state.base, finalSection)
                        }
                        banner = when (outcome) {
                            SaveOutcome.Success -> "Saved"
                            is SaveOutcome.Conflict ->
                                "This day's workout changed elsewhere - reloaded the latest version."
                            is SaveOutcome.Malformed -> "Can't save: ${outcome.reason}"
                            is SaveOutcome.IoError -> "Save failed: ${outcome.message}"
                        }
                        reload()
                    }
                },
            )
        }
    }
}

@Composable
private fun DateNavRow(date: LocalDate, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
        TextButton(onClick = onToday) {
            Text(date.format(DATE_LABEL_FORMAT), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onNext) { Text("›", style = MaterialTheme.typography.headlineSmall) }
    }
}

@Composable
private fun MalformedView(reason: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("This day's Workout section can't be read safely", style = MaterialTheme.typography.titleMedium)
        Text(
            "Reason: $reason",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Strong Vault won't touch this note until it's fixed by hand, so nothing here has been changed. " +
                "Open the note in your editor and make sure the Workout section ends with " +
                "\"<!-- workout:end -->\" before the next heading.",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EditableWorkout(
    section: WorkoutSection,
    config: WorkoutConfig,
    onSectionChange: (WorkoutSection) -> Unit,
    onSave: (WorkoutSection) -> Unit,
) {
    var newExerciseName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = section.day.orEmpty(),
                onValueChange = { onSectionChange(section.copy(day = it.ifBlank { null })) },
                label = { Text("Day") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (config.templates.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    config.templates.forEach { (name, exercises) ->
                        AssistChip(
                            onClick = {
                                val newExercises = if (section.exercises.isEmpty()) {
                                    exercises.map { WorkoutExercise(it) }
                                } else {
                                    section.exercises
                                }
                                onSectionChange(section.copy(day = name, exercises = newExercises))
                            },
                            label = { Text(name) },
                        )
                    }
                }
            }
        }
        HorizontalDivider()

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(section.exercises, key = { index, _ -> index }) { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onExerciseChange = { updated ->
                        onSectionChange(
                            section.copy(exercises = section.exercises.toMutableList().also { it[index] = updated }),
                        )
                    },
                    onRemove = {
                        onSectionChange(
                            section.copy(exercises = section.exercises.filterIndexed { i, _ -> i != index }),
                        )
                    },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        label = { Text("Add exercise") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val name = newExerciseName.trim()
                            if (name.isNotEmpty()) {
                                onSectionChange(section.copy(exercises = section.exercises + WorkoutExercise(name)))
                                newExerciseName = ""
                            }
                        },
                    ) { Text("Add") }
                }
                if (config.exercises.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        config.exercises.filter { name -> section.exercises.none { it.name.equals(name, true) } }
                            .forEach { name ->
                                AssistChip(
                                    onClick = {
                                        onSectionChange(section.copy(exercises = section.exercises + WorkoutExercise(name)))
                                    },
                                    label = { Text(name) },
                                )
                            }
                    }
                }
            }
        }

        HorizontalDivider()
        Button(
            onClick = { onSave(section) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) { Text("Save to vault") }
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    onExerciseChange: (WorkoutExercise) -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRemove) { Text("×", style = MaterialTheme.typography.titleLarge) }
            }
            exercise.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(WorkoutSectionWriter.renderSetLine(set).removePrefix("- "))
                    IconButton(onClick = {
                        onExerciseChange(exercise.copy(sets = exercise.sets.filterIndexed { i, _ -> i != index }))
                    }) { Text("×") }
                }
            }
            AddSetRow(onAdd = { set -> onExerciseChange(exercise.copy(sets = exercise.sets + set)) })
        }
    }
}

@Composable
private fun AddSetRow(onAdd: (WorkoutSet) -> Unit) {
    var isBodyweight by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf("") }
    var repsText by remember { mutableStateOf("") }
    var rpeText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = isBodyweight, onClick = { isBodyweight = !isBodyweight }, label = { Text("BW") })
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text(if (isBodyweight) "+/-" else "Weight") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(90.dp),
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it },
                label = { Text("Reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(70.dp),
            )
            OutlinedTextField(
                value = rpeText,
                onValueChange = { rpeText = it },
                label = { Text("RPE") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(70.dp),
            )
            IconButton(onClick = {
                val loadToken = when {
                    isBodyweight && weightText.isBlank() -> "BW"
                    isBodyweight && weightText.startsWith("-") -> "BW${weightText}"
                    isBodyweight -> "BW+${weightText.removePrefix("+")}"
                    else -> weightText
                }
                val rpeSuffix = rpeText.takeIf { it.isNotBlank() }?.let { "@$it" } ?: ""
                val line = "- ${loadToken}x${repsText}$rpeSuffix"
                val set = WorkoutSectionParser.parseSetLine(line)
                if (set == null) {
                    error = "Couldn't parse that set"
                } else {
                    error = null
                    onAdd(set)
                    repsText = ""
                    rpeText = ""
                }
            }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
