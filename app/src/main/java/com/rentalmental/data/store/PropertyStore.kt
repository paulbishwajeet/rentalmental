package com.rentalmental.data.store

import android.content.Context
import com.rentalmental.data.model.Property
import com.rentalmental.data.model.Room
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PropertyStore(context: Context) {

    private val prefs = context.getSharedPreferences("rental_mental", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getProperties(): List<Property> {
        val raw = prefs.getString("properties", null) ?: return emptyList()
        return json.decodeFromString(raw)
    }

    fun saveProperties(properties: List<Property>) {
        prefs.edit().putString("properties", json.encodeToString(properties)).apply()
    }

    fun addProperty(property: Property) {
        saveProperties(getProperties() + property)
    }

    fun updateProperty(property: Property) {
        saveProperties(getProperties().map { if (it.id == property.id) property else it })
    }

    fun deleteProperty(propertyId: String) {
        saveProperties(getProperties().filter { it.id != propertyId })
        deleteRoomsForProperty(propertyId)
    }

    fun getRooms(propertyId: String): List<Room> {
        val raw = prefs.getString("rooms_$propertyId", null) ?: return emptyList()
        return json.decodeFromString(raw)
    }

    private fun saveRooms(propertyId: String, rooms: List<Room>) {
        prefs.edit().putString("rooms_$propertyId", json.encodeToString(rooms)).apply()
    }

    fun addRoom(room: Room) {
        saveRooms(room.propertyId, getRooms(room.propertyId) + room)
    }

    fun updateRoom(room: Room) {
        saveRooms(room.propertyId, getRooms(room.propertyId).map { if (it.id == room.id) room else it })
    }

    fun deleteRoom(room: Room) {
        saveRooms(room.propertyId, getRooms(room.propertyId).filter { it.id != room.id })
    }

    private fun deleteRoomsForProperty(propertyId: String) {
        prefs.edit().remove("rooms_$propertyId").apply()
    }
}
