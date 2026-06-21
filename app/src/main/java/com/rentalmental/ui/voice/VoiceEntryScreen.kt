package com.rentalmental.ui.voice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalmental.data.sheets.SheetsClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    roomLabel: String,
    propertyName: String,
    recognizedText: String,
    isListening: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    successMessage: String?,
    history: List<SheetsClient.RowEntry>,
    isLoadingHistory: Boolean,
    hasMoreHistory: Boolean,
    onTextChanged: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSave: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(propertyName, style = MaterialTheme.typography.titleLarge)
                        Text(roomLabel, style = MaterialTheme.typography.bodySmall)
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
            if (!isSaving) {
                FloatingActionButton(
                    onClick = { if (isListening) onStopListening() else onStartListening() }
                ) {
                    if (isListening) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop recording")
                    } else {
                        Icon(Icons.Filled.Mic, contentDescription = "Record")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                if (isListening) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Listening...", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = recognizedText,
                    onValueChange = onTextChanged,
                    label = { Text("Voice entry (Hindi)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 5,
                    enabled = !isListening && !isSaving
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (successMessage != null) {
                    Text(successMessage, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translating and saving...")
                    }
                } else {
                    Row {
                        Button(
                            onClick = onSave,
                            enabled = recognizedText.isNotBlank()
                        ) {
                            Text("Save")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = onStartListening) {
                            Text("Re-record")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (history.isNotEmpty() || isLoadingHistory) {
                    Text("Recent entries", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(history) { entry ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(entry.dateTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(entry.hindiText, style = MaterialTheme.typography.bodyMedium)
                    if (entry.englishText.isNotBlank()) {
                        Text(entry.englishText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }

            if (isLoadingHistory) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading...")
                    }
                }
            }

            if (hasMoreHistory && !isLoadingHistory) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show more")
                    }
                }
            }
        }
    }
}
