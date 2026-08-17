package dev.frx.myampere.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import dev.frx.myampere.MainActivity
import dev.frx.myampere.core.BatteryRepository
import dev.frx.myampere.core.SamplingConditions
import dev.frx.myampere.core.WidgetGateState
import dev.frx.myampere.core.samplingIntervalMs
import dev.frx.myampere.core.shouldPushWidget
import dev.frx.myampere.db.AppDb
import dev.frx.myampere.db.DOWNSAMPLE_BUCKET_MS
import dev.frx.myampere.db.PURGE_AFTER_MS
import dev.frx.myampere.db.RAW_RETENTION_MS
import dev.frx.myampere.db.selectDownsampleDeletions
import dev.frx.myampere.db.toEntity
import dev.frx.myampere.widget.WidgetPusher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SamplerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var loop: Job? = null
    private lateinit var reader: BatteryReader
    private var gate = WidgetGateState(null, 0L)
    private var lastFlushMs = 0L
    private var lastMaintenanceMs = 0L

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = restartLoop()
    }

    override fun onCreate() {
        super.onCreate()
        reader = BatteryReader(this)
        registerReceiver(stateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        restartLoop()
        return START_STICKY
    }

    private fun screenOn(): Boolean =
        (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive

    private fun restartLoop() {
        loop?.cancel()
        loop = scope.launch {
            while (true) {
                try {
                    val interval = samplingIntervalMs(
                        SamplingConditions(screenOn = screenOn(), plugged = reader.isPlugged(), appVisible = appVisible)
                    ) ?: run { flushToDb(); return@launch } // stop total: receivers réveilleront

                    val now = System.currentTimeMillis()
                    val sample = reader.read(now)
                    if (sample != null) {
                        BatteryRepository.onSample(sample)
                        if (shouldPushWidget(gate, sample.currentMa, now, screenOn())) {
                            WidgetPusher.push(this@SamplerService, sample.currentMa, sample.status)
                            gate = WidgetGateState(sample.currentMa, now)
                        }
                    }
                    if (now - lastFlushMs >= FLUSH_INTERVAL_MS) { flushToDb(); lastFlushMs = now }
                    if (now - lastMaintenanceMs >= MAINTENANCE_INTERVAL_MS) { runMaintenance(now); lastMaintenanceMs = now }
                    delay(interval)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("SamplerService", "loop iteration failed", e)
                    delay(5_000)
                }
            }
        }
    }

    private suspend fun flushToDb() {
        val pending = BatteryRepository.drainPendingForDb()
        if (pending.isNotEmpty()) AppDb.get(this).sampleDao().insertAll(pending.map { it.toEntity() })
    }

    private suspend fun runMaintenance(nowMs: Long) {
        val dao = AppDb.get(this).sampleDao()
        val old = dao.timestampsBefore(nowMs - RAW_RETENTION_MS)
        val toDelete = selectDownsampleDeletions(old, DOWNSAMPLE_BUCKET_MS)
        if (toDelete.isNotEmpty()) toDelete.chunked(500).forEach { dao.deleteByTimestamps(it) }
        dao.deleteBefore(nowMs - PURGE_AFTER_MS)
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Monitoring", NotificationManager.IMPORTANCE_MIN)
        )
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("My Ampere actif")
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        loop?.cancel()
        try {
            unregisterReceiver(stateReceiver)
        } catch (e: IllegalArgumentException) {
            // receiver already unregistered
        }
        scope.launch { flushToDb() }.invokeOnCompletion { scope.cancel() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "sampler"
        private const val FLUSH_INTERVAL_MS = 60_000L
        private const val MAINTENANCE_INTERVAL_MS = 6L * 3600 * 1000

        @Volatile var appVisible: Boolean = false
        /** Toggle utilisateur : quand false, onResume/onPause ne relancent PAS le service. */
        @Volatile var userEnabled: Boolean = true

        fun start(context: Context) =
            context.startForegroundService(Intent(context, SamplerService::class.java))
        fun stop(context: Context) =
            context.stopService(Intent(context, SamplerService::class.java))
    }
}
