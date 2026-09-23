package com.example.strong_vault.workout.model

data class WorkoutSet(
    val load: Load,
    val reps: Int,
    val rpe: Double? = null,
    val note: String? = null,
)

data class WorkoutExercise(
    val name: String,
    val sets: List<WorkoutSet> = emptyList(),
    /** Lines under this exercise heading that didn't match the set grammar. Preserved verbatim on write. */
    val extraLines: List<String> = emptyList(),
)

data class WorkoutSection(
    val day: String? = null,
    /** Lines between the `**Day**` line (or section start) and the first exercise heading that didn't
     *  match any recognized grammar. Preserved verbatim on write. */
    val orphanLines: List<String> = emptyList(),
    val exercises: List<WorkoutExercise> = emptyList(),
)
