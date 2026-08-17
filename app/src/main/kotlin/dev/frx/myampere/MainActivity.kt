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

    private fun startSampler() { if (SamplerService.userEnabled) safeStartSampler() }

    /** Ne jamais crasher l'activité si le FGS refuse de démarrer (état d'app en arrière-plan,
     *  permission retirée entre deux bascules d'écran, etc.). */
    private fun safeStartSampler() {
        try {
            SamplerService.start(this)
        } catch (e: IllegalStateException) {
            // contexte d'app impropre au démarrage d'un foreground service, on ignore
        } catch (e: SecurityException) {
            // permission manquante (POST_NOTIFICATIONS révoquée entre-temps), on ignore
        }
    }

    override fun onResume() {
        super.onResume()
        SamplerService.appVisible = true
        if (SamplerService.userEnabled) safeStartSampler()
    }
    override fun onPause() {
        super.onPause()
        SamplerService.appVisible = false
        if (SamplerService.userEnabled) safeStartSampler()
    }
}
