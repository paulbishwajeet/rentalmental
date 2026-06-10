package com.rentalmental.ui.rentallist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rentalmental.data.model.Rental

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalListScreen(
    rentals: List<Rental>,
    onRentalSelected: (Rental) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rental Mental") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(rentals, key = { it.id }) { rental ->
                ListItem(
                    headlineContent = { Text(rental.label) },
                    supportingContent = {
                        Text("Floor ${rental.floor} · Capacity ${rental.capacity} · Rent ₹${rental.expectedRent}")
                    },
                    modifier = Modifier.clickable { onRentalSelected(rental) }
                )
            }
        }
    }
}
