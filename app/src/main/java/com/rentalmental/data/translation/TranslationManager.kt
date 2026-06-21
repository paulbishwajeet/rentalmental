package com.rentalmental.data.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TranslationManager {

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.HINDI)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    private var modelReady = false

    suspend fun ensureModelDownloaded() {
        if (modelReady) return
        suspendCoroutine { cont ->
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    modelReady = true
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(Exception("Failed to download translation model: ${e.message}"))
                }
        }
    }

    suspend fun translate(hindiText: String): String {
        ensureModelDownloaded()
        return suspendCoroutine { cont ->
            translator.translate(hindiText)
                .addOnSuccessListener { result -> cont.resume(result) }
                .addOnFailureListener { e ->
                    cont.resumeWithException(Exception("Translation failed: ${e.message}"))
                }
        }
    }

    fun close() {
        translator.close()
    }
}
