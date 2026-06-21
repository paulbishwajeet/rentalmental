package com.rentalmental.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val id: String,
    val name: String,
    val address: String,
    val spreadsheetId: String? = null
)

@Serializable
data class Room(
    val id: String,
    val propertyId: String,
    val label: String,
    val floor: Int,
    val expectedRent: Int,
    val tenantName: String,
    val securityDeposit: Int
)
