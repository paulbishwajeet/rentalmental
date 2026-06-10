package com.rentalmental.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rentalmental.ui.AppViewModel
import com.rentalmental.ui.ProcessingState
import com.rentalmental.ui.journal.JournalScreen
import com.rentalmental.ui.rentallist.RentalListScreen
import com.rentalmental.ui.review.ReviewScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()

    val rentals by viewModel.rentals.collectAsState()
    val journalText by viewModel.journalText.collectAsState()
    val processingState by viewModel.processingState.collectAsState()

    NavHost(navController = navController, startDestination = "rentalList") {
        composable("rentalList") {
            RentalListScreen(
                rentals = rentals,
                onRentalSelected = { rental ->
                    viewModel.selectRental(rental.id)
                    navController.navigate("journal")
                }
            )
        }
        composable("journal") {
            val rental = viewModel.selectedRental ?: return@composable
            val state = processingState

            if (state is ProcessingState.ReadyForReview) {
                ReviewScreen(
                    draft = state.draft,
                    initialReminder = state.reminder,
                    onConfirm = { entry, reminder -> viewModel.confirmEntry(entry, reminder) },
                    onDiscard = { viewModel.discard() },
                    onReRecord = {
                        viewModel.discard()
                        viewModel.startRecording()
                    }
                )
            } else {
                JournalScreen(
                    rental = rental,
                    journalText = journalText,
                    processingState = state,
                    onRecordToggle = {
                        if (state is ProcessingState.Recording) {
                            viewModel.stopRecordingAndProcess()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    onRetry = { viewModel.retryProcessing() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
