package com.rentalmental.data.gemini

import com.rentalmental.data.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiResponseParserTest {

    @Test
    fun `parse extracts all fields from a well-formed response`() {
        val raw = """
            {
              "transcription": "Aaj Prakash ne 2000 rupaye diye hai rent ke. Baaki bola hai kal dega.",
              "entry_type": "rent_payment",
              "person": "Prakash",
              "amount": 2000,
              "payment_method": "cash",
              "summary": "Partial rent payment. Remaining ₹3000 promised for tomorrow.",
              "reminder": {
                "has_reminder": true,
                "due_date": "2026-06-11",
                "description": "Follow up with Prakash for remaining ₹3000 rent"
              }
            }
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        result!!
        assertEquals("Prakash", result.person)
        assertEquals(EntryType.RENT_PAYMENT, result.entryType)
        assertEquals(2000, result.amount)
        assertEquals("cash", result.paymentMethod)
        assertNotNull(result.reminder)
        assertEquals("2026-06-11", result.reminder!!.dueDate)
        assertEquals("Follow up with Prakash for remaining ₹3000 rent", result.reminder!!.description)
    }

    @Test
    fun `parse handles response wrapped in markdown code fences`() {
        val raw = """
            ```json
            {
              "transcription": "test",
              "entry_type": "expense",
              "person": "Owner",
              "amount": 500,
              "payment_method": null,
              "summary": "Bought cleaning supplies.",
              "reminder": null
            }
            ```
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        result!!
        assertEquals(EntryType.EXPENSE, result.entryType)
        assertEquals(500, result.amount)
        assertNull(result.paymentMethod)
        assertNull(result.reminder)
    }

    @Test
    fun `parse returns null reminder when has_reminder is false`() {
        val raw = """
            {
              "transcription": "test",
              "entry_type": "other",
              "person": "Sunita",
              "amount": null,
              "payment_method": null,
              "summary": "General note.",
              "reminder": { "has_reminder": false, "due_date": null, "description": null }
            }
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        assertNull(result!!.reminder)
    }

    @Test
    fun `parse returns null for malformed JSON`() {
        val result = GeminiResponseParser.parse("not valid json")

        assertNull(result)
    }

    @Test
    fun `parse falls back to OTHER for unknown entry_type`() {
        val raw = """
            {
              "transcription": "test",
              "entry_type": "something_unexpected",
              "person": "Owner",
              "amount": null,
              "payment_method": null,
              "summary": "note",
              "reminder": null
            }
        """.trimIndent()

        val result = GeminiResponseParser.parse(raw)

        assertNotNull(result)
        assertEquals(EntryType.OTHER, result!!.entryType)
    }
}
