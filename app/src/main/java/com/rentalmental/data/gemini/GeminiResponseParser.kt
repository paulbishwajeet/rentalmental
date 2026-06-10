package com.rentalmental.data.gemini

import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.GeminiExtraction
import com.rentalmental.data.model.Reminder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GeminiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawText: String): GeminiExtraction? {
        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = json.parseToJsonElement(cleaned).jsonObject

            val transcription = obj["transcription"]?.jsonPrimitive?.contentOrNull ?: ""
            val entryType = EntryType.fromKey(obj["entry_type"]?.jsonPrimitive?.contentOrNull)
            val person = obj["person"]?.jsonPrimitive?.contentOrNull ?: ""
            val amount = obj["amount"]?.jsonPrimitive?.intOrNull
            val paymentMethod = obj["payment_method"]?.jsonPrimitive?.contentOrNull
            val summary = obj["summary"]?.jsonPrimitive?.contentOrNull ?: ""

            val reminderObj = obj["reminder"] as? JsonObject
            val reminder = if (reminderObj != null &&
                reminderObj["has_reminder"]?.jsonPrimitive?.booleanOrNull == true
            ) {
                Reminder(
                    dueDate = reminderObj["due_date"]?.jsonPrimitive?.contentOrNull ?: "",
                    description = reminderObj["description"]?.jsonPrimitive?.contentOrNull ?: ""
                )
            } else null

            GeminiExtraction(
                transcription = transcription,
                entryType = entryType,
                person = person,
                amount = amount,
                paymentMethod = paymentMethod,
                summary = summary,
                reminder = reminder
            )
        } catch (e: Exception) {
            null
        }
    }
}
