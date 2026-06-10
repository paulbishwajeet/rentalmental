package com.rentalmental.data.seed

import android.content.Context
import com.rentalmental.data.model.Rental
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RentalDto(
    val id: String,
    val label: String,
    val floor: Int,
    val expected_rent: Int,
    val capacity: Int,
    val rooms: String,
    val electricity_responsibility: String,
    val security_deposit: Int
)

object SeedDataLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseRentals(jsonText: String): List<Rental> {
        val dtos = json.decodeFromString<List<RentalDto>>(jsonText)
        return dtos.map {
            Rental(
                id = it.id,
                label = it.label,
                floor = it.floor,
                expectedRent = it.expected_rent,
                capacity = it.capacity,
                rooms = it.rooms,
                electricityResponsibility = it.electricity_responsibility,
                securityDeposit = it.security_deposit
            )
        }
    }

    fun loadFromAssets(context: Context): List<Rental> {
        val jsonText = context.assets.open("rentals_seed.json")
            .bufferedReader()
            .use { it.readText() }
        return parseRentals(jsonText)
    }
}
