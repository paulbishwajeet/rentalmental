package com.rentalmental.data.seed

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedDataLoaderTest {

    @Test
    fun `parseRentals maps JSON fields to Rental objects`() {
        val json = """
            [
              {
                "id": "F0-R1",
                "label": "Ground Floor - Room 1",
                "floor": 0,
                "expected_rent": 5000,
                "capacity": 2,
                "rooms": "1 room + attached bath, no kitchen",
                "electricity_responsibility": "tenant",
                "security_deposit": 10000
              },
              {
                "id": "F1-R1",
                "label": "First Floor - Room 1",
                "floor": 1,
                "expected_rent": 6000,
                "capacity": 3,
                "rooms": "2 rooms + shared bath + kitchen",
                "electricity_responsibility": "tenant",
                "security_deposit": 12000
              }
            ]
        """.trimIndent()

        val rentals = SeedDataLoader.parseRentals(json)

        assertEquals(2, rentals.size)
        assertEquals("F0-R1", rentals[0].id)
        assertEquals("Ground Floor - Room 1", rentals[0].label)
        assertEquals(0, rentals[0].floor)
        assertEquals(5000, rentals[0].expectedRent)
        assertEquals(2, rentals[0].capacity)
        assertEquals("1 room + attached bath, no kitchen", rentals[0].rooms)
        assertEquals("tenant", rentals[0].electricityResponsibility)
        assertEquals(10000, rentals[0].securityDeposit)
        assertEquals("F1-R1", rentals[1].id)
        assertEquals(3, rentals[1].capacity)
    }
}
