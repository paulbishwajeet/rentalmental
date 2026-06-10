package com.rentalmental.data.gemini

import com.rentalmental.data.model.Rental
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPromptBuilderTest {

    private val rental = Rental(
        id = "F0-R1",
        label = "Ground Floor - Room 1",
        floor = 0,
        expectedRent = 5000,
        capacity = 2,
        rooms = "1 room + attached bath, no kitchen",
        electricityResponsibility = "tenant",
        securityDeposit = 10000
    )

    @Test
    fun `buildPrompt includes rental details, date, and JSON schema`() {
        val prompt = GeminiPromptBuilder.buildPrompt(rental, recentJournalText = "", todayDate = "2026-06-10")

        assertTrue(prompt.contains("Ground Floor - Room 1"))
        assertTrue(prompt.contains("₹5000"))
        assertTrue(prompt.contains("2026-06-10"))
        assertTrue(prompt.contains("\"transcription\""))
        assertTrue(prompt.contains("\"entry_type\""))
        assertTrue(prompt.contains("\"reminder\""))
    }

    @Test
    fun `buildPrompt includes recent journal context when provided`() {
        val prompt = GeminiPromptBuilder.buildPrompt(
            rental,
            recentJournalText = "### 2026-06-09 10:00 — Rent Payment\n- Person: Prakash",
            todayDate = "2026-06-10"
        )

        assertTrue(prompt.contains("Recent journal entries"))
        assertTrue(prompt.contains("### 2026-06-09 10:00 — Rent Payment"))
    }

    @Test
    fun `buildPrompt omits recent journal section when empty`() {
        val prompt = GeminiPromptBuilder.buildPrompt(rental, recentJournalText = "", todayDate = "2026-06-10")

        assertFalse(prompt.contains("Recent journal entries"))
    }
}
