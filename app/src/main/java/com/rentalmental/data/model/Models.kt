package com.rentalmental.data.model

data class Rental(
    val id: String,
    val label: String,
    val floor: Int,
    val expectedRent: Int,
    val capacity: Int,
    val rooms: String,
    val electricityResponsibility: String,
    val securityDeposit: Int
)

enum class EntryType(val label: String) {
    RENT_PAYMENT("Rent Payment"),
    ELECTRICITY_PAYMENT("Electricity Payment"),
    EXPENSE("Expense"),
    OTHER("Other");

    companion object {
        fun fromKey(key: String?): EntryType =
            entries.find { it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}

data class JournalEntry(
    val date: String,
    val time: String,
    val type: EntryType,
    val person: String,
    val amount: Int?,
    val paymentMethod: String?,
    val summary: String,
    val rawTranscription: String
)

data class Reminder(
    val dueDate: String,
    val description: String,
    val done: Boolean = false
)

data class GeminiExtraction(
    val transcription: String,
    val entryType: EntryType,
    val person: String,
    val amount: Int?,
    val paymentMethod: String?,
    val summary: String,
    val reminder: Reminder?
)
