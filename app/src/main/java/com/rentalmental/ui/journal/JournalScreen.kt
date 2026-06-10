package com.rentalmental.ui.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalmental.data.model.Rental
import com.rentalmental.ui.ProcessingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    rental: Rental,
    journalText: String,
    processingState: ProcessingState,
    onRecordToggle: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rental.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onRecordToggle) {
                if (processingState is ProcessingState.Recording) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop recording")
                } else {
                    Icon(Icons.Filled.Mic, contentDescription = "Record")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Expected rent: ₹${rental.expectedRent} · Capacity: ${rental.capacity}")
            Spacer(modifier = Modifier.height(8.dp))

            if (processingState is ProcessingState.Processing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listening and processing...")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (processingState is ProcessingState.Failed) {
                Text("Error: ${processingState.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = journalText,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            )
        }
    }
}
