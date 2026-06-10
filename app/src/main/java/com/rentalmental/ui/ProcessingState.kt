package com.rentalmental.ui

import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder

sealed interface ProcessingState {
    object Idle : ProcessingState
    object Recording : ProcessingState
    object Processing : ProcessingState
    data class ReadyForReview(val draft: JournalEntry, val reminder: Reminder?) : ProcessingState
    data class Failed(val message: String, val rawTranscription: String?) : ProcessingState
}
