package com.rentalmental.ui.property

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.Property

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    properties: List<Property>,
    onPropertySelected: (Property) -> Unit,
    onAddProperty: (name: String, address: String) -> Unit,
    onEditProperty: (Property) -> Unit,
    onDeleteProperty: (Property) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProperty by remember { mutableStateOf<Property?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rental Mental") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add property")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(properties, key = { it.id }) { property ->
                ListItem(
                    headlineContent = { Text(property.name) },
                    supportingContent = { Text(property.address) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editingProperty = property }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDeleteProperty(property) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onPropertySelected(property) }
                )
            }
        }
    }

    if (showAddDialog) {
        PropertyDialog(
            title = "Add Property",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, address ->
                onAddProperty(name, address)
                showAddDialog = false
            }
        )
    }

    editingProperty?.let { property ->
        PropertyDialog(
            title = "Edit Property",
            initialName = property.name,
            initialAddress = property.address,
            onDismiss = { editingProperty = null },
            onConfirm = { name, address ->
                onEditProperty(property.copy(name = name, address = address))
                editingProperty = null
            }
        )
    }
}

@Composable
private fun PropertyDialog(
    title: String,
    initialName: String = "",
    initialAddress: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Property Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, address) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
