package com.rentalmental.data.journal

import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class JournalRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repository: JournalRepository

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

    @Before
    fun setUp() {
        tempDir = File.createTempFile("journals", "").apply {
            delete()
            mkdirs()
        }
        repository = JournalRepository(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `initializeIfNeeded creates a markdown file per rental`() {
        repository.initializeIfNeeded(listOf(rental))

        val file = File(tempDir, "F0-R1.md")
        assertTrue(file.exists())
        assertTrue(file.readText().contains("rental_id: F0-R1"))
    }

    @Test
    fun `initializeIfNeeded does not overwrite an existing file`() {
        repository.initializeIfNeeded(listOf(rental))
        val file = File(tempDir, "F0-R1.md")
        file.appendText("\nextra content")

        repository.initializeIfNeeded(listOf(rental))

        assertTrue(file.readText().contains("extra content"))
    }

    @Test
    fun `appendEntry writes the entry to the rental's journal file`() {
        repository.initializeIfNeeded(listOf(rental))
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "14:32",
            type = EntryType.RENT_PAYMENT,
            person = "Prakash",
            amount = 2000,
            paymentMethod = "cash",
            summary = "Partial rent payment.",
            rawTranscription = "raw"
        )

        repository.appendEntry("F0-R1", entry)

        assertTrue(repository.readJournal("F0-R1").contains("Prakash"))
    }

    @Test
    fun `appendReminderToJournal writes the reminder to the rental's journal file`() {
        repository.initializeIfNeeded(listOf(rental))
        repository.appendReminderToJournal("F0-R1", Reminder("2026-06-11", "Follow up with Prakash"))

        val content = repository.readJournal("F0-R1")
        assertTrue(content.contains("- [ ] 2026-06-11 — Follow up with Prakash"))
    }

    @Test
    fun `recentContext returns recent journal entries for the rental`() {
        repository.initializeIfNeeded(listOf(rental))
        val entry = JournalEntry(
            date = "2026-06-10",
            time = "14:32",
            type = EntryType.RENT_PAYMENT,
            person = "Prakash",
            amount = 2000,
            paymentMethod = "cash",
            summary = "Partial rent payment.",
            rawTranscription = "raw"
        )
        repository.appendEntry("F0-R1", entry)

        val context = repository.recentContext("F0-R1", maxEntries = 5)

        assertTrue(context.contains("Prakash"))
    }

    @Test
    fun `readJournal returns empty string for unknown rental`() {
        assertEquals("", repository.readJournal("UNKNOWN"))
    }
}
