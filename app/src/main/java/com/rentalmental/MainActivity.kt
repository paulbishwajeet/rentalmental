package com.rentalmental

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.rentalmental.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* no-op; recording will fail gracefully if denied */ }
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            AppNavigation()
        }
    }
}
