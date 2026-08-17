package dev.frx.myampere

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import dev.frx.myampere.service.SamplerService
import dev.frx.myampere.ui.AppRoot

class MainActivity : ComponentActivity() {
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startSampler() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startSampler()
    }

    private fun startSampler() { if (SamplerService.userEnabled) SamplerService.start(this) }

    override fun onResume() {
        super.onResume()
        SamplerService.appVisible = true
        if (SamplerService.userEnabled) SamplerService.start(this)
    }
    override fun onPause() {
        super.onPause()
        SamplerService.appVisible = false
        if (SamplerService.userEnabled) SamplerService.start(this)
    }
}
