package com.rentalmental.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.rentalmental.ui.AppViewModel
import com.rentalmental.ui.auth.SignInScreen
import com.rentalmental.ui.property.PropertyListScreen
import com.rentalmental.ui.room.RoomListScreen
import com.rentalmental.ui.voice.VoiceEntryScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()

    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val voiceError by viewModel.voiceError.collectAsState()
    val voiceSuccess by viewModel.voiceSuccess.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()
    val hasMoreHistory by viewModel.hasMoreHistory.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleSignInResult(task)
        }
    }

    val startDestination = if (isSignedIn) "propertyList" else "signIn"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("signIn") {
            SignInScreen(
                onSignIn = { signInLauncher.launch(viewModel.getSignInIntent()) },
                errorMessage = authError
            )
        }

        composable("propertyList") {
            PropertyListScreen(
                properties = properties,
                onPropertySelected = { property ->
                    viewModel.selectProperty(property)
                    navController.navigate("roomList")
                },
                onAddProperty = { name, address -> viewModel.addProperty(name, address) },
                onEditProperty = { property -> viewModel.editProperty(property) },
                onDeleteProperty = { property -> viewModel.deleteProperty(property) }
            )
        }

        composable("roomList") {
            val property = viewModel.selectedProperty ?: return@composable
            RoomListScreen(
                propertyName = property.name,
                rooms = rooms,
                propertyId = property.id,
                onRoomSelected = { room ->
                    viewModel.selectRoom(room)
                    navController.navigate("voiceEntry")
                },
                onAddRoom = { room -> viewModel.addRoom(room) },
                onEditRoom = { room -> viewModel.editRoom(room) },
                onDeleteRoom = { room -> viewModel.deleteRoom(room) },
                onBack = { navController.popBackStack() }
            )
        }

        composable("voiceEntry") {
            val property = viewModel.selectedProperty ?: return@composable
            val room = viewModel.selectedRoom ?: return@composable
            VoiceEntryScreen(
                roomLabel = room.label,
                propertyName = property.name,
                recognizedText = recognizedText,
                isListening = isListening,
                isSaving = isSaving,
                errorMessage = voiceError,
                successMessage = voiceSuccess,
                history = history,
                isLoadingHistory = isLoadingHistory,
                hasMoreHistory = hasMoreHistory,
                onTextChanged = { viewModel.updateRecognizedText(it) },
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() },
                onSave = { viewModel.saveEntry() },
                onLoadMore = { viewModel.loadMoreHistory() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
