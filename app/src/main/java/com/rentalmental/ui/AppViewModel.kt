package com.rentalmental.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.rentalmental.data.auth.GoogleAuthManager
import com.rentalmental.data.model.Property
import com.rentalmental.data.model.Room
import com.rentalmental.data.sheets.SheetsClient
import com.rentalmental.data.speech.SpeechRecognizerManager
import com.rentalmental.data.store.PropertyStore
import com.rentalmental.data.translation.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = GoogleAuthManager(application)
    private val propertyStore = PropertyStore(application)
    private val sheetsClient = SheetsClient(application)
    private val speechManager = SpeechRecognizerManager(application)
    private val translationManager = TranslationManager()

    private val _isSignedIn = MutableStateFlow(authManager.isSignedIn(application))
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> = _properties

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError

    private val _voiceSuccess = MutableStateFlow<String?>(null)
    val voiceSuccess: StateFlow<String?> = _voiceSuccess

    var selectedProperty: Property? = null
        private set
    var selectedRoom: Room? = null
        private set

    init {
        loadProperties()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                translationManager.ensureModelDownloaded()
            } catch (_: Exception) { }
        }
    }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            task.getResult(ApiException::class.java)
            _isSignedIn.update { true }
            _authError.update { null }
        } catch (e: ApiException) {
            _authError.update { "Sign-in failed: ${e.statusCode}" }
        }
    }

    private fun loadProperties() {
        _properties.update { propertyStore.getProperties() }
    }

    fun addProperty(name: String, address: String) {
        val account = authManager.getAccount(getApplication()) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val spreadsheetId = sheetsClient.createSpreadsheet(account.email!!, "RentalMental - $name")
                val property = Property(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    address = address,
                    spreadsheetId = spreadsheetId
                )
                propertyStore.addProperty(property)
                loadProperties()
            } catch (e: Exception) {
                _authError.update { "Failed to create spreadsheet: ${e.message}" }
            }
        }
    }

    fun editProperty(property: Property) {
        propertyStore.updateProperty(property)
        loadProperties()
    }

    fun deleteProperty(property: Property) {
        propertyStore.deleteProperty(property.id)
        loadProperties()
    }

    fun selectProperty(property: Property) {
        selectedProperty = property
        _rooms.update { propertyStore.getRooms(property.id) }
    }

    fun addRoom(room: Room) {
        val property = selectedProperty ?: return
        val spreadsheetId = property.spreadsheetId ?: return
        val account = authManager.getAccount(getApplication()) ?: return

        propertyStore.addRoom(room)
        _rooms.update { propertyStore.getRooms(property.id) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                sheetsClient.addSheet(account.email!!, spreadsheetId, room.label)
            } catch (e: Exception) {
                _voiceError.update { "Room saved locally but sheet tab creation failed: ${e.message}" }
            }
        }
    }

    fun editRoom(room: Room) {
        val oldRoom = propertyStore.getRooms(room.propertyId).find { it.id == room.id }
        propertyStore.updateRoom(room)
        _rooms.update { propertyStore.getRooms(room.propertyId) }

        if (oldRoom != null && oldRoom.label != room.label) {
            val property = selectedProperty ?: return
            val spreadsheetId = property.spreadsheetId ?: return
            val account = authManager.getAccount(getApplication()) ?: return
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    sheetsClient.renameSheet(account.email!!, spreadsheetId, oldRoom.label, room.label)
                } catch (e: Exception) {
                    _voiceError.update { "Room renamed locally but sheet rename failed: ${e.message}" }
                }
            }
        }
    }

    fun deleteRoom(room: Room) {
        propertyStore.deleteRoom(room)
        _rooms.update { propertyStore.getRooms(room.propertyId) }
    }

    fun selectRoom(room: Room) {
        selectedRoom = room
        _recognizedText.update { "" }
        _voiceError.update { null }
        _voiceSuccess.update { null }
    }

    fun updateRecognizedText(text: String) {
        _recognizedText.update { text }
    }

    fun startListening() {
        _voiceError.update { null }
        _voiceSuccess.update { null }
        _isListening.update { true }

        speechManager.startListening(
            onPartialResult = { text -> _recognizedText.update { text } },
            onFinalResult = { text ->
                _recognizedText.update { text }
                _isListening.update { false }
            },
            onError = { message ->
                _voiceError.update { message }
                _isListening.update { false }
            }
        )
    }

    fun stopListening() {
        speechManager.stop()
        _isListening.update { false }
    }

    fun saveEntry() {
        val property = selectedProperty ?: return
        val room = selectedRoom ?: return
        val spreadsheetId = property.spreadsheetId ?: return
        val account = authManager.getAccount(getApplication()) ?: return
        val hindiText = _recognizedText.value
        if (hindiText.isBlank()) return

        _isSaving.update { true }
        _voiceError.update { null }
        _voiceSuccess.update { null }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val englishText = try {
                    translationManager.translate(hindiText)
                } catch (_: Exception) {
                    "(translation unavailable)"
                }

                val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                sheetsClient.insertRow(account.email!!, spreadsheetId, room.label, dateTime, hindiText, englishText)

                _voiceSuccess.update { "Entry saved!" }
                _recognizedText.update { "" }
            } catch (e: Exception) {
                _voiceError.update { "Save failed: ${e.message}" }
            } finally {
                _isSaving.update { false }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stop()
        translationManager.close()
    }
}
