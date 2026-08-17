package dev.frx.myampere.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.frx.myampere.MainActivity
import dev.frx.myampere.R
import dev.frx.myampere.core.ChargeStatus
import dev.frx.myampere.core.Prefs
import dev.frx.myampere.core.statusLabelShort
import kotlinx.coroutines.runBlocking

object WidgetPusher {
    private const val PERSIST_THROTTLE_MS = 60_000L
    @Volatile private var lastPersistMs = 0L

    fun push(context: Context, currentMa: Int, status: ChargeStatus) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val color = if (currentMa >= 0) context.getColor(R.color.charge_green)
                    else context.getColor(R.color.discharge_red)
        val views = RemoteViews(context.packageName, R.layout.widget_battery).apply {
            setTextViewText(R.id.widget_value, "$currentMa mA")
            setTextColor(R.id.widget_value, color)
            setTextViewText(R.id.widget_label, statusLabelShort(status))
        }
        mgr.partiallyUpdateAppWidget(ids, views)
        // Persistance throttlée (1/min) : la valeur ne sert qu'au fallback grisé,
        // une écriture disque par push (≤10 s) contredirait le budget conso.
        val now = System.currentTimeMillis()
        if (now - lastPersistMs >= PERSIST_THROTTLE_MS) {
            lastPersistMs = now
            runBlocking { Prefs.setLastMa(context, currentMa) }
        }
    }

    /** Fallback quand le service ne tourne pas : dernière valeur connue, grisée. */
    fun pushStale(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val last = runBlocking { Prefs.lastMa(context) }
        val pi = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val views = RemoteViews(context.packageName, R.layout.widget_battery).apply {
            setTextViewText(R.id.widget_value, if (last != null) "$last mA" else "— mA")
            setTextColor(R.id.widget_value, context.getColor(R.color.stale_grey))
            setTextViewText(R.id.widget_label, "inactif")
            setOnClickPendingIntent(R.id.widget_root, pi)
        }
        mgr.updateAppWidget(ids, views) // full update: pose aussi le click handler
    }
}
