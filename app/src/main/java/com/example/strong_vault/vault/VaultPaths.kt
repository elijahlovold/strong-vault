package com.example.strong_vault.vault

import java.io.File
import java.time.LocalDate

/** Mirrors androidhomecal's own layout convention for this vault: <vault>/Calendar/<year>/<date>.md */
object VaultPaths {
    fun dailyNoteFile(vaultRoot: File, date: LocalDate): File =
        File(vaultRoot, "Calendar/${date.year}/$date.md")

    fun calendarYearDir(vaultRoot: File, year: Int): File =
        File(vaultRoot, "Calendar/$year")

    fun workoutConfigFile(vaultRoot: File): File =
        File(vaultRoot, "Personal/Workout.md")
}
