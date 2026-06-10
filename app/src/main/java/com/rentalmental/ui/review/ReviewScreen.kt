package com.rentalmental.ui.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.EntryType
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    draft: JournalEntry,
    initialReminder: Reminder?,
    onConfirm: (JournalEntry, Reminder?) -> Unit,
    onDiscard: () -> Unit,
    onReRecord: () -> Unit
) {
    var person by remember { mutableStateOf(draft.person) }
    var amountText by remember { mutableStateOf(draft.amount?.toString() ?: "") }
    var paymentMethod by remember { mutableStateOf(draft.paymentMethod ?: "") }
    var summary by remember { mutableStateOf(draft.summary) }
    var entryType by remember { mutableStateOf(draft.type) }
    var includeReminder by remember { mutableStateOf(initialReminder != null) }
    var reminderDate by remember { mutableStateOf(initialReminder?.dueDate ?: "") }
    var reminderDesc by remember { mutableStateOf(initialReminder?.description ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Review entry") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text("Transcription (Hindi)", style = MaterialTheme.typography.labelLarge)
            Text(draft.rawTranscription, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = entryType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    EntryType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                entryType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = person,
                onValueChange = { person = it },
                label = { Text("Person") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment method (cash/online)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeReminder, onCheckedChange = { includeReminder = it })
                Text("Add reminder")
            }
            if (includeReminder) {
                OutlinedTextField(
                    value = reminderDate,
                    onValueChange = { reminderDate = it },
                    label = { Text("Reminder date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reminderDesc,
                    onValueChange = { reminderDesc = it },
                    label = { Text("Reminder description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Button(onClick = {
                    val entry = draft.copy(
                        type = entryType,
                        person = person,
                        amount = amountText.toIntOrNull(),
                        paymentMethod = paymentMethod.takeUnless { it.isBlank() },
                        summary = summary
                    )
                    val reminder = if (includeReminder && reminderDate.isNotBlank() && reminderDesc.isNotBlank()) {
                        Reminder(dueDate = reminderDate, description = reminderDesc)
                    } else null
                    onConfirm(entry, reminder)
                }) {
                    Text("Confirm")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onDiscard) {
                    Text("Discard")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onReRecord) {
                    Text("Re-record")
                }
            }
        }
    }
}
