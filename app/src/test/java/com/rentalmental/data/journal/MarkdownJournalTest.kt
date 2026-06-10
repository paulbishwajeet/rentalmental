package com.rentalmental.data.journal

import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownJournalTest {

    private fun sampleRental() = Rental(
        id = "F0-R1",
        label = "Ground Floor - Room 1",
        floor = 0,
        expectedRent = 5000,
        capacity = 2,
        rooms = "1 room + attached bath, no kitchen",
        electricityResponsibility = "tenant",
        securityDeposit = 10000
    )

    private fun sampleEntry(time: String, person: String) = JournalEntry(
        date = "2026-06-10",
        time = time,
        type = EntryType.RENT_PAYMENT,
        person = person,
        amount = 2000,
        paymentMethod = "cash",
        summary = "Partial rent payment.",
        rawTranscription = "raw transcription"
    )

    @Test
    fun `generateInitialMarkdown contains frontmatter and section headings`() {
        val markdown = generateInitialMarkdown(sampleRental())

        assertTrue(markdown.startsWith("---\n"))
        assertTrue(markdown.contains("rental_id: F0-R1"))
        assertTrue(markdown.contains("label: Ground Floor - Room 1"))
        assertTrue(markdown.contains("expected_rent: 5000"))
        assertTrue(markdown.contains("\n# Journal\n"))
        assertTrue(markdown.contains("\n# Reminders"))
        assertTrue(markdown.indexOf("# Journal") < markdown.indexOf("# Reminders"))
    }

    @Test
    fun `appendJournalEntry inserts entry block before Reminders section`() {
        val initial = generateInitialMarkdown(sampleRental())
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "14:32",
            type = EntryType.RENT_PAYMENT,
            person = "Prakash",
            amount = 2000,
            paymentMethod = "cash",
            summary = "Partial rent payment. Remaining ₹3000 promised for tomorrow.",
            rawTranscription = "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega."
        )

        val updated = appendJournalEntry(initial, entry)

        assertTrue(updated.contains("### 2026-06-10 14:32 — Rent Payment"))
        assertTrue(updated.contains("- Person: Prakash"))
        assertTrue(updated.contains("- Amount: ₹2000 (cash)"))
        assertTrue(updated.contains("- Note: Partial rent payment. Remaining ₹3000 promised for tomorrow."))
        assertTrue(updated.contains("- Raw (Hindi): \"Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega.\""))
        assertTrue(updated.indexOf("### 2026-06-10 14:32") < updated.indexOf("# Reminders"))
        assertTrue(updated.indexOf("# Journal") < updated.indexOf("### 2026-06-10 14:32"))
    }

    @Test
    fun `appendJournalEntry omits amount line when amount is null`() {
        val initial = generateInitialMarkdown(sampleRental())
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "09:00",
            type = EntryType.OTHER,
            person = "Sunita",
            amount = null,
            paymentMethod = null,
            summary = "Asked about water leakage in bathroom.",
            rawTranscription = "Sunita ne bola bathroom mein paani leak ho raha hai."
        )

        val updated = appendJournalEntry(initial, entry)

        assertFalse(updated.contains("- Amount:"))
        assertTrue(updated.contains("- Note: Asked about water leakage in bathroom."))
    }

    @Test
    fun `appendJournalEntry can append multiple entries in order`() {
        var markdown = generateInitialMarkdown(sampleRental())
        markdown = appendJournalEntry(markdown, sampleEntry(time = "09:00", person = "Sunita"))
        markdown = appendJournalEntry(markdown, sampleEntry(time = "18:00", person = "Prakash"))

        val firstIndex = markdown.indexOf("Sunita")
        val secondIndex = markdown.indexOf("Prakash")
        assertTrue(firstIndex in 0 until secondIndex)
        assertTrue(secondIndex < markdown.indexOf("# Reminders"))
    }

    @Test
    fun `appendReminder adds checklist item after Reminders heading`() {
        val initial = generateInitialMarkdown(sampleRental())
        val reminder = Reminder(
            dueDate = "2026-06-11",
            description = "Follow up with Prakash for remaining ₹3000 rent"
        )

        val updated = appendReminder(initial, reminder)

        assertTrue(updated.contains("- [ ] 2026-06-11 — Follow up with Prakash for remaining ₹3000 rent"))
        assertTrue(updated.indexOf("# Reminders") < updated.indexOf("- [ ] 2026-06-11"))
    }

    @Test
    fun `appendReminder can append multiple reminders`() {
        var markdown = generateInitialMarkdown(sampleRental())
        markdown = appendReminder(markdown, Reminder("2026-06-11", "First reminder"))
        markdown = appendReminder(markdown, Reminder("2026-06-12", "Second reminder"))

        assertTrue(markdown.contains("- [ ] 2026-06-11 — First reminder"))
        assertTrue(markdown.contains("- [ ] 2026-06-12 — Second reminder"))
        assertTrue(markdown.indexOf("First reminder") < markdown.indexOf("Second reminder"))
    }

    @Test
    fun `extractRecentJournalText returns most recent entries up to the limit`() {
        var markdown = generateInitialMarkdown(sampleRental())
        markdown = appendJournalEntry(markdown, sampleEntry(time = "08:00", person = "Person1"))
        markdown = appendJournalEntry(markdown, sampleEntry(time = "09:00", person = "Person2"))
        markdown = appendJournalEntry(markdown, sampleEntry(time = "10:00", person = "Person3"))

        val recent = extractRecentJournalText(markdown, maxEntries = 2)

        assertFalse(recent.contains("Person1"))
        assertTrue(recent.contains("Person2"))
        assertTrue(recent.contains("Person3"))
        assertFalse(recent.contains("# Reminders"))
    }

    @Test
    fun `extractRecentJournalText returns empty string when no entries exist`() {
        val markdown = generateInitialMarkdown(sampleRental())

        val recent = extractRecentJournalText(markdown, maxEntries = 5)

        assertEquals("", recent)
    }
}
