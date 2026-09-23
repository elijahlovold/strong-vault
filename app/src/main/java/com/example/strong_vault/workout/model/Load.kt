package com.example.strong_vault.workout.model

/** The weight moved on a single set. */
sealed class Load {
    /** An absolute external weight (barbell, dumbbell, machine stack, ...), in the vault's configured unit. */
    data class Absolute(val weight: Double) : Load()

    /** Bodyweight, optionally with added load (weighted vest/belt, positive) or assistance (negative). */
    data class Bodyweight(val delta: Double = 0.0) : Load()
}
