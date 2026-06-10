package com.rentalmental.data.journal

import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental

private const val JOURNAL_HEADING = "# Journal"
private const val REMINDERS_HEADING = "# Reminders"

fun generateInitialMarkdown(rental: Rental): String {
    return buildString {
        appendLine("---")
        appendLine("rental_id: ${rental.id}")
        appendLine("label: ${rental.label}")
        appendLine("floor: ${rental.floor}")
        appendLine("expected_rent: ${rental.expectedRent}")
        appendLine("capacity: ${rental.capacity}")
        appendLine("rooms: ${rental.rooms}")
        appendLine("electricity_responsibility: ${rental.electricityResponsibility}")
        appendLine("security_deposit: ${rental.securityDeposit}")
        appendLine("---")
        appendLine()
        appendLine(JOURNAL_HEADING)
        appendLine()
        appendLine(REMINDERS_HEADING)
    }
}

private fun formatEntryBlock(entry: JournalEntry): String {
    return buildString {
        appendLine("### ${entry.date} ${entry.time} — ${entry.type.label}")
        appendLine("- Person: ${entry.person}")
        if (entry.amount != null) {
            val method = entry.paymentMethod?.let { " ($it)" } ?: ""
            appendLine("- Amount: ₹${entry.amount}$method")
        }
        appendLine("- Note: ${entry.summary}")
        append("- Raw (Hindi): \"${entry.rawTranscription}\"")
    }
}

fun appendJournalEntry(markdown: String, entry: JournalEntry): String {
    val lines = markdown.lines().toMutableList()
    val reminderIndex = lines.indexOfFirst { it.trim() == REMINDERS_HEADING }
    require(reminderIndex >= 0) { "Markdown missing '$REMINDERS_HEADING' section" }

    val toInsert = mutableListOf<String>()
    toInsert.addAll(formatEntryBlock(entry).lines())
    toInsert.add("")

    val insertAt = if (reminderIndex > 0 && lines[reminderIndex - 1].isNotBlank()) {
        lines.add(reminderIndex, "")
        reminderIndex + 1
    } else {
        reminderIndex
    }

    lines.addAll(insertAt, toInsert)
    return lines.joinToString("\n")
}

fun appendReminder(markdown: String, reminder: Reminder): String {
    val lines = markdown.lines().toMutableList()
    val reminderIndex = lines.indexOfFirst { it.trim() == REMINDERS_HEADING }
    require(reminderIndex >= 0) { "Markdown missing '$REMINDERS_HEADING' section" }

    val checkbox = if (reminder.done) "[x]" else "[ ]"
    lines.add("- $checkbox ${reminder.dueDate} — ${reminder.description}")
    return lines.joinToString("\n")
}

fun extractRecentJournalText(markdown: String, maxEntries: Int): String {
    val lines = markdown.lines()
    val journalIndex = lines.indexOfFirst { it.trim() == JOURNAL_HEADING }
    val reminderIndex = lines.indexOfFirst { it.trim() == REMINDERS_HEADING }
    if (journalIndex < 0 || reminderIndex < 0 || reminderIndex <= journalIndex) return ""

    val journalLines = lines.subList(journalIndex + 1, reminderIndex)
    val entryStarts = journalLines.withIndex()
        .filter { (_, line) -> line.trim().startsWith("### ") }
        .map { it.index }

    if (entryStarts.isEmpty()) return ""

    val firstIncluded = entryStarts.takeLast(maxEntries).first()
    return journalLines.subList(firstIncluded, journalLines.size)
        .joinToString("\n")
        .trim()
}
