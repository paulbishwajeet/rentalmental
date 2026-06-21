package com.rentalmental.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.Property
import com.rentalmental.ui.theme.cardColorFor

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
        if (properties.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No properties yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add your rental properties to start recording\nvoice notes for each room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Tap + to add your first property",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(properties, key = { _, p -> p.id }) { index, property ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColorFor(index))
                            .clickable { onPropertySelected(property) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(property.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                property.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { editingProperty = property }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDeleteProperty(property) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
