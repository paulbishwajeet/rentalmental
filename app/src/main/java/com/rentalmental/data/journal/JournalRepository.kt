package com.rentalmental.data.journal

import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import java.io.File

class JournalRepository(private val journalsDir: File) {

    init {
        if (!journalsDir.exists()) journalsDir.mkdirs()
    }

    private fun fileFor(rentalId: String) = File(journalsDir, "$rentalId.md")

    fun initializeIfNeeded(rentals: List<Rental>) {
        for (rental in rentals) {
            val file = fileFor(rental.id)
            if (!file.exists()) {
                file.writeText(generateInitialMarkdown(rental))
            }
        }
    }

    fun readJournal(rentalId: String): String {
        val file = fileFor(rentalId)
        return if (file.exists()) file.readText() else ""
    }

    fun appendEntry(rentalId: String, entry: JournalEntry) {
        val file = fileFor(rentalId)
        file.writeText(appendJournalEntry(file.readText(), entry))
    }

    fun appendReminderToJournal(rentalId: String, reminder: Reminder) {
        val file = fileFor(rentalId)
        file.writeText(appendReminder(file.readText(), reminder))
    }

    fun recentContext(rentalId: String, maxEntries: Int = 5): String {
        return extractRecentJournalText(readJournal(rentalId), maxEntries)
    }
}
