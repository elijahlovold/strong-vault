package com.example.strong_vault.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutConfigParserTest {

    @Test
    fun `parses exercises and templates`() {
        val text = """
            # Exercises

            - Bench Press
            - Squat

            # Templates

            ## Push
            - Bench Press

            ## Legs
            - Squat
        """.trimIndent()

        val config = WorkoutConfigParser.parse(text)
        assertEquals(listOf("Bench Press", "Squat"), config.exercises)
        assertEquals(listOf("Bench Press"), config.templates["Push"])
        assertEquals(listOf("Squat"), config.templates["Legs"])
    }

    @Test
    fun `tolerates a leading YAML frontmatter block`() {
        val text = """
            ---
            type: workout-config
            ---

            # Exercises

            - Bench Press

            # Templates

            ## Push
            - Bench Press
        """.trimIndent()

        val config = WorkoutConfigParser.parse(text)
        assertEquals(listOf("Bench Press"), config.exercises)
        assertEquals(listOf("Bench Press"), config.templates["Push"])
    }

    @Test
    fun `null text falls back to defaults`() {
        val config = WorkoutConfigParser.parse(null)
        assertEquals(com.example.strong_vault.workout.model.WorkoutConfig.DEFAULT, config)
    }

    @Test
    fun `render then parse round trips`() {
        val config = com.example.strong_vault.workout.model.WorkoutConfig.DEFAULT
        val rendered = WorkoutConfigParser.render(config)
        val reparsed = WorkoutConfigParser.parse(rendered)
        assertEquals(config, reparsed)
    }
}
