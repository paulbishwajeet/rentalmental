package com.rentalmental.ui.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    propertyName: String,
    rooms: List<Room>,
    propertyId: String,
    onRoomSelected: (Room) -> Unit,
    onAddRoom: (Room) -> Unit,
    onEditRoom: (Room) -> Unit,
    onDeleteRoom: (Room) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(propertyName, style = MaterialTheme.typography.titleLarge)
                        Text("Rooms", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add room")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(rooms, key = { it.id }) { room ->
                ListItem(
                    headlineContent = { Text(room.label) },
                    supportingContent = {
                        Text("Floor ${room.floor} · ${room.tenantName} · Rent ₹${room.expectedRent}")
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editingRoom = room }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDeleteRoom(room) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onRoomSelected(room) }
                )
            }
        }
    }

    if (showAddDialog) {
        RoomDialog(
            title = "Add Room",
            propertyId = propertyId,
            onDismiss = { showAddDialog = false },
            onConfirm = { room ->
                onAddRoom(room)
                showAddDialog = false
            }
        )
    }

    editingRoom?.let { room ->
        RoomDialog(
            title = "Edit Room",
            propertyId = propertyId,
            initial = room,
            onDismiss = { editingRoom = null },
            onConfirm = { updated ->
                onEditRoom(updated)
                editingRoom = null
            }
        )
    }
}

@Composable
private fun RoomDialog(
    title: String,
    propertyId: String,
    initial: Room? = null,
    onDismiss: () -> Unit,
    onConfirm: (Room) -> Unit
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var floor by remember { mutableStateOf(initial?.floor?.toString() ?: "0") }
    var expectedRent by remember { mutableStateOf(initial?.expectedRent?.toString() ?: "") }
    var tenantName by remember { mutableStateOf(initial?.tenantName ?: "") }
    var securityDeposit by remember { mutableStateOf(initial?.securityDeposit?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Room Label (e.g. Room 1)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it.filter { c -> c.isDigit() } },
                    label = { Text("Floor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tenantName,
                    onValueChange = { tenantName = it },
                    label = { Text("Tenant Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = expectedRent,
                    onValueChange = { expectedRent = it.filter { c -> c.isDigit() } },
                    label = { Text("Expected Rent (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = securityDeposit,
                    onValueChange = { securityDeposit = it.filter { c -> c.isDigit() } },
                    label = { Text("Security Deposit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val room = Room(
                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        propertyId = propertyId,
                        label = label,
                        floor = floor.toIntOrNull() ?: 0,
                        expectedRent = expectedRent.toIntOrNull() ?: 0,
                        tenantName = tenantName,
                        securityDeposit = securityDeposit.toIntOrNull() ?: 0
                    )
                    onConfirm(room)
                },
                enabled = label.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
