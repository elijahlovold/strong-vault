package com.example.strong_vault.stats

import com.example.strong_vault.workout.model.Load
import com.example.strong_vault.workout.model.WorkoutSection
import com.example.strong_vault.workout.model.WorkoutSet
import java.time.LocalDate

data class ExercisePoint(
    val date: LocalDate,
    val topWeight: Double,
    val estimated1Rm: Double,
    val volume: Double,
)

object WorkoutStats {

    /** Bodyweight sets without a known bodyweight can't be expressed in absolute-weight terms;
     *  they're simply excluded from weight-based stats rather than guessed at. */
    private fun WorkoutSet.absoluteWeightOrNull(): Double? = when (val l = load) {
        is Load.Absolute -> l.weight
        is Load.Bodyweight -> null
    }

    /** Epley formula: a standard, simple estimated-1RM approximation. */
    private fun estimated1Rm(weight: Double, reps: Int): Double =
        if (reps <= 1) weight else weight * (1 + reps / 30.0)

    fun exerciseHistory(
        sections: List<Pair<LocalDate, WorkoutSection>>,
        exerciseName: String,
    ): List<ExercisePoint> = sections
        .mapNotNull { (date, section) ->
            val exercise = section.exercises.find { it.name.equals(exerciseName, ignoreCase = true) }
                ?: return@mapNotNull null
            val weightedSets = exercise.sets.mapNotNull { set ->
                set.absoluteWeightOrNull()?.let { it to set.reps }
            }
            if (weightedSets.isEmpty()) return@mapNotNull null
            val topWeight = weightedSets.maxOf { it.first }
            val best1Rm = weightedSets.maxOf { (weight, reps) -> estimated1Rm(weight, reps) }
            val volume = weightedSets.sumOf { (weight, reps) -> weight * reps }
            ExercisePoint(date, topWeight, best1Rm, volume)
        }
        .sortedBy { it.date }

    fun distinctExerciseNames(sections: List<Pair<LocalDate, WorkoutSection>>): List<String> =
        sections.flatMap { it.second.exercises.map { ex -> ex.name } }
            .distinctBy { it.lowercase() }
            .sorted()

    /** Total set count per day - the input to a frequency heatmap. */
    fun dailySetCounts(sections: List<Pair<LocalDate, WorkoutSection>>): Map<LocalDate, Int> =
        sections.associate { (date, section) -> date to section.exercises.sumOf { it.sets.size } }
}
