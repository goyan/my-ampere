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
import dev.frx.myampere.core.Prefs
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
import kotlinx.coroutines.runBlocking

class SamplerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var loop: Job? = null
    private lateinit var reader: BatteryReader
    private var gate = WidgetGateState(null, 0L)
    private var lastFlushMs = 0L
    private var lastMaintenanceMs = 0L
    @Volatile private var fullUpdatePosted = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = restartLoop()
    }

    override fun onCreate() {
        super.onCreate()
        reader = BatteryReader(this)
        lastMaintenanceMs = System.currentTimeMillis()
        // Une seule lecture bloquante ici, et une seule fois par process (resyncedFromDisk) :
        // resynchronise le toggle utilisateur avec sa valeur persistée avant tout démarrage.
        // Sans ce garde-fou, un onCreate ulterieur du meme process (le service peut etre
        // recree plusieurs fois sans que le process ne meure) ecraserait une valeur memoire
        // a jour (mise par setUserEnabled) avec une lecture disque potentiellement en retard.
        if (!resyncedFromDisk) {
            userEnabled = runBlocking { Prefs.userEnabled(this@SamplerService) }
            resyncedFromDisk = true
        }
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
        if (!fullUpdatePosted) {
            fullUpdatePosted = true
            scope.launch { WidgetPusher.pushStale(this@SamplerService) }
        }
        if (!userEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
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
                            WidgetPusher.push(this@SamplerService, sample.currentMa, sample.status, sample.levelPct, sample.voltageMv)
                            gate = WidgetGateState(sample.currentMa, now)
                        }
                    } else {
                        BatteryRepository.onUnsupportedSample()
                    }
                    if (BatteryRepository.unsupported.value) { flushToDb(); return@launch }
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
        val cutoff = nowMs - RAW_RETENTION_MS
        val old = dao.timestampsBetween(cutoff - MAINTENANCE_WINDOW_MS, cutoff)
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
        WidgetPusher.pushStale(this)
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
        private const val MAINTENANCE_WINDOW_MS = 7L * 24 * 3600 * 1000

        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile var appVisible: Boolean = false
        /** Toggle utilisateur : quand false, onResume/onPause ne relancent PAS le service.
         *  Source de vérité durable : [Prefs.userEnabled] (DataStore) — ce champ n'est qu'un
         *  cache mémoire, resynchronisé depuis le disque une seule fois par process (voir
         *  [resyncedFromDisk]) puis considéré à jour ensuite. */
        @Volatile var userEnabled: Boolean = true
        /** Armé après la première resync disque du process (onCreate) ou par [setUserEnabled] :
         *  au-delà, la valeur mémoire fait foi, on ne relit plus le disque. */
        @Volatile private var resyncedFromDisk: Boolean = false

        fun start(context: Context) =
            context.startForegroundService(Intent(context, SamplerService::class.java))
        fun stop(context: Context) =
            context.stopService(Intent(context, SamplerService::class.java))

        /** Change le toggle utilisateur : persiste puis démarre/arrête le service en conséquence. */
        fun setUserEnabled(context: Context, enabled: Boolean) {
            userEnabled = enabled
            resyncedFromDisk = true
            ioScope.launch { Prefs.setUserEnabled(context, enabled) }
            if (enabled) start(context) else stop(context)
        }
    }
}
