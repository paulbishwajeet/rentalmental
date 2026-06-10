package com.rentalmental.data.gemini

import com.rentalmental.data.model.Rental

object GeminiPromptBuilder {

    fun buildPrompt(rental: Rental, recentJournalText: String, todayDate: String): String {
        return buildString {
            appendLine("You are an assistant that helps an Indian landlord journal rental activity.")
            appendLine("Today's date is $todayDate (format YYYY-MM-DD).")
            appendLine()
            appendLine("Rental details:")
            appendLine("- Label: ${rental.label}")
            appendLine("- Expected monthly rent: ₹${rental.expectedRent}")
            appendLine("- Capacity: ${rental.capacity}")
            appendLine("- Electricity responsibility: ${rental.electricityResponsibility}")
            appendLine()
            if (recentJournalText.isNotBlank()) {
                appendLine("Recent journal entries for context:")
                appendLine(recentJournalText)
                appendLine()
            }
            appendLine("Listen to the attached audio, which is in Hindi. Transcribe it, then extract a structured journal entry.")
            appendLine("Respond with ONLY a JSON object matching this schema (no markdown fences):")
            appendLine(
                """
                {
                  "transcription": string,
                  "entry_type": one of "rent_payment" | "electricity_payment" | "expense" | "other",
                  "person": string,
                  "amount": number or null,
                  "payment_method": "cash" | "online" | null,
                  "summary": string,
                  "reminder": {
                    "has_reminder": boolean,
                    "due_date": "YYYY-MM-DD" or null,
                    "description": string or null
                  } or null
                }
                """.trimIndent()
            )
        }
    }
}
