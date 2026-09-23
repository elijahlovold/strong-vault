package com.example.strong_vault.workout.model

/** Parsed contents of the vault's Workout.md: the master exercise list and named day templates. */
data class WorkoutConfig(
    val exercises: List<String> = emptyList(),
    val templates: Map<String, List<String>> = emptyMap(),
) {
    companion object {
        val DEFAULT = WorkoutConfig(
            exercises = listOf(
                "Bench Press", "Overhead Press", "Incline Dumbbell Press", "Triceps Pushdown",
                "Deadlift", "Barbell Row", "Pull-up", "Barbell Curl",
                "Squat", "Leg Press", "Calf Raise",
            ),
            templates = linkedMapOf(
                "Push" to listOf("Bench Press", "Overhead Press", "Triceps Pushdown"),
                "Pull" to listOf("Deadlift", "Barbell Row", "Pull-up", "Barbell Curl"),
                "Legs" to listOf("Squat", "Leg Press", "Calf Raise"),
            ),
        )
    }
}
