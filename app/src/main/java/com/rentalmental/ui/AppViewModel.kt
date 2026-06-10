package com.rentalmental.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rentalmental.data.audio.AudioRecorder
import com.rentalmental.data.gemini.GeminiClient
import com.rentalmental.data.gemini.GeminiPromptBuilder
import com.rentalmental.data.gemini.GeminiResponseParser
import com.rentalmental.data.journal.JournalRepository
import com.rentalmental.data.model.JournalEntry
import com.rentalmental.data.model.Reminder
import com.rentalmental.data.model.Rental
import com.rentalmental.data.seed.SeedDataLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JournalRepository(File(application.filesDir, "journals"))
    private val geminiClient = GeminiClient()
    private val audioRecorder = AudioRecorder(application)

    private val _rentals = MutableStateFlow<List<Rental>>(emptyList())
    val rentals: StateFlow<List<Rental>> = _rentals

    private val _journalText = MutableStateFlow("")
    val journalText: StateFlow<String> = _journalText

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    var selectedRental: Rental? = null
        private set

    private var lastAudioFile: File? = null
    private var lastPrompt: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = SeedDataLoader.loadFromAssets(application)
            repository.initializeIfNeeded(loaded)
            _rentals.update { loaded }
        }
    }

    fun selectRental(rentalId: String) {
        selectedRental = _rentals.value.find { it.id == rentalId }
        refreshJournal()
    }

    private fun refreshJournal() {
        val rental = selectedRental ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _journalText.update { repository.readJournal(rental.id) }
        }
    }

    fun startRecording() {
        _processingState.update { ProcessingState.Recording }
        audioRecorder.startRecording()
    }

    fun stopRecordingAndProcess() {
        val rental = selectedRental ?: return
        _processingState.update { ProcessingState.Processing }
        viewModelScope.launch(Dispatchers.IO) {
            val audioFile = audioRecorder.stopRecording()
            lastAudioFile = audioFile

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val recentContext = repository.recentContext(rental.id)
            val prompt = GeminiPromptBuilder.buildPrompt(rental, recentContext, today)
            lastPrompt = prompt

            processWithGemini(audioFile, prompt)
        }
    }

    fun retryProcessing() {
        val audioFile = lastAudioFile
        val prompt = lastPrompt
        if (audioFile == null || prompt == null) {
            _processingState.update { ProcessingState.Idle }
            return
        }
        _processingState.update { ProcessingState.Processing }
        viewModelScope.launch(Dispatchers.IO) {
            processWithGemini(audioFile, prompt)
        }
    }

    private fun processWithGemini(audioFile: File, prompt: String) {
        try {
            val rawResponse = geminiClient.transcribeAndExtract(audioFile, prompt)
            val extraction = GeminiResponseParser.parse(rawResponse)

            if (extraction == null) {
                _processingState.update {
                    ProcessingState.Failed(
                        "Could not understand the response. Please fill in details manually.",
                        rawResponse
                    )
                }
            } else {
                val now = Date()
                val draft = JournalEntry(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
                    time = SimpleDateFormat("HH:mm", Locale.US).format(now),
                    type = extraction.entryType,
                    person = extraction.person,
                    amount = extraction.amount,
                    paymentMethod = extraction.paymentMethod,
                    summary = extraction.summary,
                    rawTranscription = extraction.transcription
                )
                _processingState.update { ProcessingState.ReadyForReview(draft, extraction.reminder) }
            }
        } catch (e: Exception) {
            _processingState.update { ProcessingState.Failed(e.message ?: "Unknown error", null) }
        }
    }

    fun confirmEntry(entry: JournalEntry, reminder: Reminder?) {
        val rental = selectedRental ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.appendEntry(rental.id, entry)
            if (reminder != null) {
                repository.appendReminderToJournal(rental.id, reminder)
            }
            refreshJournal()
            _processingState.update { ProcessingState.Idle }
        }
    }

    fun discard() {
        _processingState.update { ProcessingState.Idle }
    }
}
